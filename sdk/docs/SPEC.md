<title>SPEC — 이미지 검증 SDK</title>

# SPEC — 이미지 검증 SDK

버전 0.1 (초안). 구현 전 문서다. 이 문서와 다르게 구현할 이유가 생기면
**코드가 아니라 이 문서를 먼저 고친다.**

---

## 0. 한 줄 정의

> 기준 이미지 여러 장과 후보 이미지 한 장을 받아, 가장 잘 맞는 기준 이미지와
> 그 유사도, 그리고 임계값 통과 여부를 돌려준다.

그게 전부다. 이 SDK 는 그 이미지가 왜 비교되는지 모른다.

## 1. 범위

### 1.1 SDK 안에 들어가는 것

- 추론 서버와의 HTTP 통신, 타임아웃, 재시도 없음 정책
- 여러 기준 이미지에 대한 병렬/배치 추론 디스패치
- 임베딩 캐시 (기준 이미지 한정)
- 유사도 계산 (기본: 코사인)
- 최고 점수 선택
- 임계값 비교
- 설정 값 객체와 Spring Boot 자동 구성
- SDK 전용 예외 계층

### 1.2 SDK 밖에 남는 것 — 절대 들어오지 않는다

| 남는 것 | 이유 |
|---|---|
| 주문·티켓·환불·유저·메뉴 | ReviewTicket 도메인 |
| DB 트랜잭션 경계 | 호출자 소관 |
| 이미지 저장·읽기, 파일 경로, `/uploads` URL 체계 | 저장소는 SDK 밖 (ARCH-R5) |
| 리사이즈, 최소 긴 변 검사, JPEG 재인코딩 | 애플리케이션 정책 |
| HTTP 상태코드 매핑 (422/503), `errorCode` 문자열 | 애플리케이션 API 계약 |
| "표본 사진은 5칸" 같은 개수 규칙 | 애플리케이션 스키마 |
| DINOv2 라는 이름 | 교체 가능해야 한다 (ARCH-R4) |

---

## 2. 공개 API

Maven group `com.reviewticket`, 패키지 루트 `com.reviewticket.sdk.imageverify`.
**[결정 D-1 확정됨.]**

group 이 애플리케이션과 같지만 경계는 흐려지지 않는다. `com.reviewticket.sdk.*`
는 ReviewTicket 을 모르는 코드이고 `com.reviewticket.server.*` 가 ReviewTicket
자체다. 가드레일 GR-02 는 후자만 정확히 겨냥한다.

### 2.1 진입점

```java
package com.reviewticket.sdk.imageverify.api;

public interface ImageVerifier {

    /**
     * 후보 이미지가 기준 이미지 중 하나와 일치하는지 판정한다.
     *
     * @param references 기준 이미지 1장 이상. key 는 고유하고 안정적이어야 한다(§4.2)
     * @param candidate  후보 이미지 원본 바이트. SDK 는 리사이즈·변환하지 않는다
     * @throws IllegalArgumentException      입력 계약 위반(§3.1) — 호출자의 버그
     * @throws InferenceUnavailableException 추론 백엔드 장애·타임아웃(§5)
     * @throws InvalidImageException         추론 백엔드가 이미지를 디코드하지 못함
     */
    VerificationResult verify(List<ReferenceImage> references, byte[] candidate);

    /**
     * 기준 이미지의 임베딩을 미리 계산해 캐시에 채운다. 판정은 하지 않는다.
     * 캐시가 꺼져 있으면 아무 일도 하지 않는다. Phase 3 부터 유효하다.
     */
    default void warmUp(List<ReferenceImage> references) { }
}
```

### 2.2 입력 타입

```java
public final class ReferenceImage {

    /** 바이트를 지연 로딩한다. 캐시 적중 시 loader 는 호출되지 않는다 — 이게 캐시의 실익이다. */
    public static ReferenceImage of(String key, Supplier<byte[]> loader);

    /** 이미 바이트를 들고 있을 때. */
    public static ReferenceImage ofBytes(String key, byte[] bytes);

    public String key();

    /** loader 를 최대 한 번만 호출하고 결과를 보관한다. */
    public byte[] bytes();
}
```

`Supplier<byte[]>` 로 받는 것이 저장소를 숨기는 핵심 장치다. SDK 는 URL 도 파일도
모른 채 "필요할 때 부를 함수" 하나만 쥔다.

### 2.3 출력 타입

```java
public record VerificationResult(
        boolean matched,        // best >= threshold
        double similarity,      // 최고 점수. 원본 값 그대로, 클램프하지 않는다
        String matchedKey,      // 최고 점수를 낸 기준 이미지의 key. null 아님
        double threshold,       // 판정에 쓰인 임계값. 응답을 로깅·재현할 때 필요하다
        List<Score> scores      // 전체 점수. 입력 순서 유지
) {}

public record Score(String key, double similarity) {}
```

### 2.4 확장 지점 (SPI)

```java
package com.reviewticket.sdk.imageverify.spi;

/** 임베딩 백엔드. DINOv2 든 무엇이든 이 인터페이스만 만족하면 된다. */
public interface EmbeddingModel {
    /** 캐시 키에 섞인다. 모델이 바뀌면 이 값이 바뀌어 캐시가 자동 무효화된다(§4.1). */
    String modelId();
    int dimension();
    /** 입력 순서와 같은 순서로, L2 정규화된 벡터를 돌려준다. */
    List<float[]> embed(List<byte[]> images);
}

/** 임베딩을 노출하지 않고 쌍 단위 점수만 주는 백엔드. 레거시 /similarity 용. */
public interface PairwiseModel {
    String modelId();
    double similarity(byte[] a, byte[] b);
}

public interface SimilarityMetric {
    double between(float[] a, float[] b);   // 기본 구현: 코사인
}

public interface EmbeddingCache {
    float[] get(String key);
    void put(String key, float[] embedding);
    void clear();
}
```

네 인터페이스 모두 사용자가 자기 구현으로 갈아끼울 수 있다. Starter 는
`@ConditionalOnMissingBean` 으로만 기본값을 등록한다.

---

## 3. 입출력 계약

### 3.1 입력 — 위반 시 `IllegalArgumentException`

| 대상 | 계약 |
|---|---|
| `references` | null 아님, 1개 이상, `maxReferences` 이하 (기본 16) |
| `references[].key` | null·공백 아님, 목록 내 유일 |
| `references[].bytes()` | null·빈 배열 아님 |
| `candidate` | null·빈 배열 아님 |

`references` 가 비어 있는 경우를 예외로 두는 이유 — 현재 애플리케이션은
`ReviewTransaction.prepare` 에서 `MENU_SAMPLE_MISSING` 으로 미리 걸러낸다.
SDK 가 이 상황을 "불일치"로 조용히 처리하면 그 검사가 사라졌을 때 아무도
모른다. 프로그래밍 오류로 취급해 시끄럽게 터뜨린다.

이미지 형식 검증은 하지 않는다. SDK 는 바이트를 해석하지 않고 그대로 넘긴다.
디코드 실패는 추론 서버가 알려 준다(§5).

### 3.2 출력

| 항목 | 계약 |
|---|---|
| `matched` | `similarity >= threshold`. **부등호 방향 고정** — 현행 `< threshold → 거부`와 동일 |
| `similarity` | 백엔드가 준 값 그대로. 반올림·클램프 없음 |
| `matchedKey` | 최고 점수의 key. 동점이면 **입력 순서상 앞선 것** |
| `scores` | 입력과 같은 순서, 같은 길이 |

동점 규칙을 명시하는 이유 — 현행 `Stream.max(comparingDouble(...))` 는
`BinaryOperator.maxBy` 특성상 동점에서 앞선 원소를 유지한다. 저장되는
`compare_image_url` 이 바뀌면 안 되므로 이 미묘한 동작을 계약으로 고정한다.

---

## 4. 캐싱 요구사항

### 4.1 무엇을 캐시하는가

- **기준 이미지의 임베딩만** 캐시한다. 후보 이미지는 매번 새로 계산한다 —
  매 요청 다른 사진이라 캐시해도 적중하지 않고 메모리만 먹는다.
- 캐시 키: `modelId + "|" + referenceKey`
  - `modelId` 를 섞으므로 모델을 갈아끼우면 캐시가 저절로 무효가 된다.
    별도 무효화 코드가 필요 없다.
- 저장소: 프로세스 내 메모리, 크기 상한 있는 LRU. **Redis 쓰지 않는다.**
- 스레드 안전해야 한다.

### 4.2 키 안정성 계약 — 호출자의 책임

> **key 뒤의 바이트가 바뀌면 호출자는 반드시 새 key 를 써야 한다.**

SDK 는 바이트가 바뀌었는지 알 방법이 없다. 이 계약이 깨지면 낡은 임베딩으로
계속 판정한다.

ReviewTicket 에서는 이 계약이 공짜로 성립한다. `ImageStorage.save()` 가 항상
새 `UUID.jpg` 를 만들고, 메뉴 수정은 `Menu.applyEdit` 이 URL 목록을 통째로
갈아끼운다. 즉 **표본 사진 URL 은 사실상 내용 주소**라 그대로 key 로 쓸 수 있다.
(이 성질이 깨지면 캐시가 조용히 틀린다 — GUARDRAILS 의 수동 점검 항목이다.)

### 4.3 관측 가능성

캐시 적중/미적중은 테스트에서 관찰 가능해야 한다(AC-31). `ReferenceImage` 의
loader 호출 횟수로 관찰한다 — 적중이면 0회, 미적중이면 1회. 별도 통계 API 를
Phase 3 범위에 넣지 않는다.

---

## 5. 오류 동작

```
ImageVerifyException (RuntimeException)      SDK 예외의 뿌리
├── InferenceUnavailableException            연결 실패, 타임아웃, 5xx, 빈 응답
├── InvalidImageException                    추론 서버가 이미지 디코드 실패 (4xx)
└── ConfigurationException                   설정값이 유효 범위 밖
```

| 상황 | 던지는 것 |
|---|---|
| 추론 서버 미기동 / 연결 거부 | `InferenceUnavailableException` |
| `timeout` 초과 | `InferenceUnavailableException` |
| 5xx 응답 | `InferenceUnavailableException` |
| 본문이 비었거나 파싱 불가 | `InferenceUnavailableException` |
| 응답 벡터 길이가 `dimension()` 과 불일치 | `InferenceUnavailableException` |
| 4xx 응답 (디코드 실패 등) | `InvalidImageException` |
| 기준 이미지 여러 장 중 **하나라도** 실패 | 전체 실패. 첫 예외를 그대로 던진다 |
| `threshold` 가 [-1, 1] 밖 | `ConfigurationException` (생성 시점) |

**재시도하지 않는다.** 현행 동작이 그렇고, 재시도는 호출자가 결정할 정책이다.

**부분 성공을 허용하지 않는다.** 5장 중 2장만 응답했을 때 최댓값을 쓰면 통과해야
할 사진이 조용히 거부된다 — 사용자에게는 "다른 음식을 찍었다"로 보인다. 장애는
장애로 보이는 편이 낫다. 현행 `CompletableFuture::join` 동작과도 같다.

**애플리케이션 예외를 절대 던지지 않는다.** `ServiceUnavailableException`,
`ImageNotMatchedException` 은 SDK 가 이름조차 알지 못한다(GR-06). 번역은
애플리케이션 쪽 어댑터가 한다.

---

## 6. 설정

java-core 는 불변 값 객체 `VerifierConfig` 를 받는다. Spring 을 모른다.
Starter 가 프로퍼티를 바인딩해 이 객체를 만든다.

| 프로퍼티 (`image-verifier.*`) | 기본값 | 비고 |
|---|---|---|
| `backend` | `pairwise` → Phase 3 에서 `embedding` | 롤백 스위치(§7) |
| `base-url` | `http://localhost:8000` | |
| `similarity-path` | `/similarity` | 레거시 경로 |
| `embed-path` | `/embed` | Phase 2 신설 |
| `timeout` | `10s` | 현행 `reviewticket.ai.timeout` 과 동일 |
| `match-threshold` | `0.80` | 현행과 동일 |
| `parallelism` | `5` | 현행 고정 스레드풀 5와 동일 |
| `max-references` | `16` | 현행 사용량은 5 |
| `cache.enabled` | `true` | Phase 3 |
| `cache.max-entries` | `10000` | |
| `cache.ttl` | `24h` | `0` = 만료 없음 |

**[결정 D-2] java-core 는 외부 의존성 0개를 유지한다.** Caffeine 을 쓰지 않고
LRU 를 직접 구현한다(약 60줄). 상용 SDK 에서 "의존성 없음"은 도입 장벽을 크게
낮추고, 우리가 필요한 건 Caffeine 기능의 극히 일부다.

같은 이유로 JSON 라이브러리도 쓰지 않는다 — `/embed` 응답을 JSON 이 아니라
**float32 리틀엔디언 바이너리**로 정의하고(§8) 모델 정보는 헤더로 받는다.
파서가 필요 없고, 전송량도 1/4 로 줄어든다.

---

## 7. 성능 기대치

실측 기준선 (`ai/README.md`): 쌍당 평균 0.295초, 최대 3.5초.
현행 리뷰 1건 = **임베딩 10회** (5쌍 × 2장), HTTP 왕복 5회.

| 단계 | 임베딩 횟수 | HTTP 왕복 | 목표 |
|---|---|---|---|
| 현행 | 10 | 5 | 기준선 |
| Phase 1 | 10 | 5 | 회귀 없음. p95 가 기준선을 넘지 않을 것 |
| Phase 3 (콜드) | 6 | 1 | 현행보다 빠를 것 |
| Phase 3 (웜) | **1** | 1 | p95 ≤ 1.0초 |

웜 캐시가 정상 상태다. 표본 사진은 사장이 메뉴를 고칠 때만 바뀌므로, 두 번째
리뷰부터는 항상 적중한다.

---

## 8. 추론 서버 계약 (python-inference)

### 8.1 `POST /similarity` — 레거시, 유지

요청: `multipart/form-data`, 파트 `reviewImage`, `compareImage`
응답: `{"similarity": <double>}`

**Phase 5 검증이 끝나기 전에는 지우지 않는다.** 롤백 경로다.

### 8.2 `POST /embed` — Phase 2 신설

요청: `multipart/form-data`, 파트 이름 `images` 를 N번 반복
응답: `200 application/octet-stream`

| 헤더 | 값 |
|---|---|
| `X-Model-Id` | 예: `facebook/dinov2-base` — SDK 는 이 값을 캐시 키에만 쓰고 해석하지 않는다 |
| `X-Embedding-Dim` | 예: `768` |
| `X-Embedding-Count` | 입력 개수와 같아야 한다 |

본문: `count × dim` 개의 float32, 리틀엔디언, 입력 순서. **L2 정규화된 상태로
서버가 보낸다** — 정규화 위치를 한 곳으로 고정해야 Java/Python 양쪽 값이 어긋나지
않는다.

오류: 디코드 실패 시 `400`, 모델 미로딩 시 `503`.

---

## 9. 하위 호환 요구사항

**BC-1. 외부 HTTP 계약 불변.** `POST /api/reviews` 의 성공 응답, `422
IMAGE_NOT_MATCHED` + `imageSimilarity`, `503 AI_SERVER_UNAVAILABLE` 이 모두
그대로다. 프론트엔드는 한 줄도 고치지 않는다.

**BC-2. 유사도 값의 연속성.** Phase 2 에서 코사인 계산이 torch 에서 Java 로
옮겨간다. 같은 입력에 대해 **절대 오차 ≤ 1e-5** 여야 한다(AC-21). DB
`image_similarity` 컬럼에 쌓인 과거 값과 새 값이 같은 척도여야 하고,
0.80 임계값 근처에서 판정이 뒤집히면 안 된다.

**BC-3. 런타임 롤백.** `image-verifier.backend=pairwise` 한 줄로 Phase 1
동작(레거시 `/similarity` 경유)으로 즉시 되돌아간다. 재배포 없이 가능하다.

**BC-4. 설정 키 유예.** `reviewticket.ai.*` 는 Phase 4 완료까지 계속 동작한다.
Phase 5 에서만 제거한다.

**BC-5. `/similarity` 존속.** Phase 5 검증 완료 + 명시적 승인 전까지 유지한다.

---

## 10. 미결 결정

| ID | 내용 | 결정 | 상태 |
|---|---|---|---|
| D-1 | 패키지·그룹 이름 | group `com.reviewticket`, 패키지 `com.reviewticket.sdk.imageverify`, 아티팩트 `imageverify-core` | **확정** |
| D-2 | java-core 의존성 정책 | 외부 의존성 0개 | **확정** |
| D-3 | 상용 배포 시 라이선스·아티팩트 버전 | 미정. 배포는 이번 범위 밖 | Phase 5 이후 |
