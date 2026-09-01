<title>ACCEPTANCE — 인수 기준</title>

# ACCEPTANCE — 인수 기준

**구현보다 먼저 쓴다.** 각 항목은 실행 가능한 테스트 하나에 대응하고, 테스트
이름은 ID 를 그대로 쓴다(`ac01_...`). 게이트에서 "인수 테스트 통과"란 해당
Phase 열에 표시된 항목이 전부 초록이라는 뜻이다.

## 테스트 도구

| 종류 | 방법 | 이유 |
|---|---|---|
| 단위 | JUnit 5 + 손으로 만든 가짜 `EmbeddingModel` | 네트워크 없이 로직만 |
| HTTP | JDK 내장 `com.sun.net.httpserver.HttpServer` 로 스텁 서버 | java-core 의존성 0개 규칙(ARCH-R1)을 테스트도 지킨다. WireMock 안 쓴다 |
| 계약 | 실제 python-inference 기동, `@EnabledIfSystemProperty` 로 옵트인 | CI 기본 실행에서는 건너뛴다 |
| 애플리케이션 | 기존 Spring Boot 테스트 슬라이스 | Phase 5 |

## 요약표

| ID | 무엇 | P1 | P2 | P3 | P4 | P5 |
|---|---|:-:|:-:|:-:|:-:|:-:|
| AC-01 | 일치하는 사진은 통과한다 | ● | ● | ● | ● | ● |
| AC-02 | 일치하지 않는 사진은 거부된다 | ● | ● | ● | ● | ● |
| AC-03 | 임계값 경계는 포함(>=)이다 | ● | ● | ● | ● | ● |
| AC-04 | 결과의 similarity 는 가공되지 않는다 | ● | ● | ● | ● | ● |
| AC-10 | 여러 기준 중 최댓값을 고른다 | ● | ● | ● | ● | ● |
| AC-11 | matchedKey 는 최댓값을 낸 기준이다 | ● | ● | ● | ● | ● |
| AC-12 | 동점이면 앞선 것이 이긴다 | ● | ● | ● | ● | ● |
| AC-13 | scores 는 입력 순서·길이를 지킨다 | ● | ● | ● | ● | ● |
| AC-14 | 기준 이미지들은 동시에 처리된다 | ● | ● | ● | ● | ● |
| AC-15 | 입력 계약 위반은 IllegalArgumentException | ● | ● | ● | ● | ● |
| AC-20 | 추론 서버 미기동 → InferenceUnavailable | ● | ● | ● | ● | ● |
| AC-21 | 타임아웃 → InferenceUnavailable | ● | ● | ● | ● | ● |
| AC-22 | 5xx → InferenceUnavailable | ● | ● | ● | ● | ● |
| AC-23 | 빈/깨진 응답 → InferenceUnavailable | ● | ● | ● | ● | ● |
| AC-24 | 4xx → InvalidImage | ● | ● | ● | ● | ● |
| AC-25 | 하나라도 실패하면 전체 실패 | ● | ● | ● | ● | ● |
| AC-26 | 재시도하지 않는다 | ● | ● | ● | ● | ● |
| AC-27 | 애플리케이션 예외를 던지지 않는다 | ● | ● | ● | ● | ● |
| AC-30 | Java 코사인 ≡ torch 코사인 (≤1e-5) | | ● | ● | ● | ● |
| AC-31 | /embed 배치 순서·차원이 보존된다 | | ● | ● | ● | ● |
| AC-32 | pairwise 와 embedding 백엔드 결과 일치 | | ● | ● | ● | ● |
| AC-40 | 캐시 미적중 시 loader 가 1회 불린다 | | | ● | ● | ● |
| AC-41 | 캐시 적중 시 loader 가 불리지 않는다 | | | ● | ● | ● |
| AC-42 | 후보 이미지는 캐시되지 않는다 | | | ● | ● | ● |
| AC-43 | modelId 가 바뀌면 캐시가 무효화된다 | | | ● | ● | ● |
| AC-44 | 웜 캐시에서 HTTP 왕복 1회, 임베딩 1개 | | | ● | ● | ● |
| AC-45 | LRU 상한을 넘지 않는다 | | | ● | ● | ● |
| AC-46 | 캐시는 동시 접근에 안전하다 | | | ● | ● | ● |
| AC-47 | warmUp 은 판정 없이 캐시만 채운다 | | | ● | ● | ● |
| AC-48 | 캐시를 꺼도 결과는 같다 | | | ● | ● | ● |
| AC-50 | 커스텀 EmbeddingModel 로 교체된다 | | | ● | ● | ● |
| AC-51 | 커스텀 SimilarityMetric 으로 교체된다 | | | ● | ● | ● |
| AC-52 | java-core 에 DINOv2 흔적이 없다 | ● | ● | ● | ● | ● |
| AC-60 | 자동 구성이 기본 빈을 올린다 | | | | ● | ● |
| AC-61 | 사용자 빈이 기본 빈을 이긴다 | | | | ● | ● |
| AC-62 | 프로퍼티가 설정에 반영된다 | | | | ● | ● |
| AC-63 | 잘못된 설정은 기동 시 실패한다 | | | | ● | ● |
| AC-70 | 레거시 /similarity 요청·응답 형태를 지킨다 (스텁) | ● | ● | ● | ● | ● |
| AC-70c | 실제 python-multipart 가 우리 본문을 받는다 (계약) | ● | ● | ● | ● | ● |
| AC-71 | backend=pairwise 로 롤백된다 | | ● | ● | ● | ● |
| AC-72 | 422 응답 형태가 그대로다 | | | | | ● |
| AC-73 | 503 응답 형태가 그대로다 | | | | | ● |
| AC-74 | 저장되는 compare_image_url 이 그대로다 | | | | | ● |

---

## 상세

### 검증 (AC-01 ~ AC-04)

**AC-01** 기준 이미지 1장, 유사도 0.91, 임계값 0.80.
→ `matched=true`, `similarity=0.91`, `matchedKey="ref-1"`.

**AC-02** 유사도 0.62, 임계값 0.80.
→ `matched=false`, `similarity=0.62`. **예외를 던지지 않는다** — 불일치는 정상
결과지 오류가 아니다. 예외로 바꾸는 건 애플리케이션의 선택이다.

**AC-03** 유사도가 임계값과 정확히 같을 때(0.80 / 0.80) → `matched=true`.
현행 `if (best < threshold) throw` 와 같은 방향이다.
0.79999999 → `false`, 0.80000001 → `true` 도 함께 검증한다.

**AC-04** 백엔드가 0.8137254901960784 를 주면 결과도 그 값 그대로다. 반올림도
클램프도 없다. (이 값이 그대로 422 응답과 DB 로 흘러간다.)

### 다중 기준 이미지 (AC-10 ~ AC-15)

**AC-10** 5장, 점수 `[0.41, 0.55, 0.88, 0.32, 0.71]` → `similarity=0.88`.

**AC-11** 위와 같은 입력에서 `matchedKey` 는 3번째 기준의 key.

**AC-12** 점수 `[0.88, 0.55, 0.88]` → `matchedKey` 는 **첫 번째**.
현행 `Stream.max` 동작을 계약으로 고정한 것이다(SPEC §3.2).

**AC-13** 입력 5장 → `scores.size()==5`, key 순서가 입력과 동일.

**AC-14** 가짜 모델이 호출당 200ms 지연. 기준 5장.
`parallelism=5` 에서 전체 소요 < 500ms (순차라면 1000ms 이상).
Phase 3 이후 배치 백엔드에서는 "HTTP 왕복 1회"로 대체 검증(AC-44).

**AC-15** 각각 `IllegalArgumentException`:
빈 `references` / null `references` / null `candidate` / 빈 `candidate` /
key 가 null·공백 / key 중복 / `maxReferences` 초과.

### 추론 실패 (AC-20 ~ AC-27)

**AC-20** 닫힌 포트를 가리키고 verify → `InferenceUnavailableException`.

**AC-21** 스텁 서버가 `timeout` 보다 오래 지연 → `InferenceUnavailableException`.
소요 시간이 `timeout + 여유` 안에 끝나는 것까지 확인한다 — 타임아웃이 실제로
동작하는지 보는 게 요점이다.

**AC-22** 스텁이 500 → `InferenceUnavailableException`.

**AC-23** 스텁이 200 에 빈 본문 / 깨진 본문 / 길이가 `dimension` 과 다른 벡터
→ 셋 다 `InferenceUnavailableException`.

**AC-24** 스텁이 400 → `InvalidImageException`.

**AC-25** 기준 5장 중 3번째만 실패 → verify 전체가 예외. 나머지 4장의 최댓값을
반환하지 않는다. (부분 성공이 왜 위험한지는 SPEC §5.)

**AC-26** 스텁이 500 을 한 번 준 뒤 200 을 준비. 요청 수를 센다 → **1회**.

**AC-27** 어떤 실패 경로에서도 던져진 예외가 `ImageVerifyException` 의 하위형이다.
`com.reviewticket` 로 시작하는 타입은 나오지 않는다.

### 임베딩 분리 (AC-30 ~ AC-32) — Phase 2

**AC-30** 동일 이미지 쌍 20개에 대해 python 의 `torch.cosine_similarity` 결과와
Java `CosineSimilarity` 결과를 비교 → 절대 오차 ≤ 1e-5.
**BC-2 의 근거가 되는 테스트다.** 계약 테스트(옵트인)로 돌린다.

**AC-31** `/embed` 에 이미지 3장을 보내면 `X-Embedding-Count=3`, 본문 길이 =
`3 × dim × 4` 바이트, 그리고 i번째 벡터가 i번째 이미지의 것이다
(서로 다른 세 이미지로 순서 뒤바뀜을 잡는다).

**AC-32** 같은 입력을 `backend=pairwise` 와 `backend=embedding` 양쪽으로 돌려
`similarity` 차이 ≤ 1e-5, `matched` 와 `matchedKey` 는 완전히 동일.
**이게 Phase 2 게이트의 핵심이다** — 통과하지 못하면 진행하지 않는다.

### 캐시 (AC-40 ~ AC-48) — Phase 3

**AC-40** 새 key 로 verify → 그 `ReferenceImage` 의 loader 호출 횟수 1.

**AC-41** 같은 key 로 두 번째 verify → 두 번째 호출에서 loader 호출 횟수 **0**.
바이트를 읽는 일 자체가 일어나지 않는 것이 캐시의 실익이다.

**AC-42** 같은 후보 이미지로 두 번 verify → 후보에 대한 임베딩 요청이 2회.
후보는 캐시하지 않는다.

**AC-43** 같은 key, 모델의 `modelId` 만 `m1`→`m2` 로 변경 → loader 가 다시
불린다. 명시적 무효화 호출 없이 그렇게 되어야 한다.

**AC-44** 기준 5장을 warmUp 한 뒤 verify → HTTP 요청 1회, 그 요청에 담긴 이미지
1장(후보만). SPEC §7 의 "웜" 목표를 그대로 옮긴 것이다.

**AC-45** `max-entries=3` 에 5개를 넣으면 항목 수가 3을 넘지 않고, 가장 오래
쓰이지 않은 것이 밀려난다.

**AC-46** 스레드 16개가 같은 key 집합으로 동시에 verify → 예외 없음, 결과 동일,
loader 총 호출 횟수가 key 개수를 넘지 않는다.

**AC-47** `warmUp` 호출 후 loader 호출 횟수 = 기준 개수, 그리고 `VerificationResult`
는 생성되지 않는다(반환형이 void). 이어지는 verify 에서 loader 는 안 불린다.

**AC-48** `cache.enabled=false` 로 같은 시나리오 → 결과가 켰을 때와 동일.
캐시는 성능 장치지 의미를 바꾸는 장치가 아니다.

### 모델 교체 (AC-50 ~ AC-52)

**AC-50** 고정 벡터를 돌려주는 `EmbeddingModel` 을 직접 만들어 주입 →
HTTP 없이 verify 가 끝까지 동작한다. **네트워크·python 없이 SDK 전체 경로가
돈다는 것 자체가 교체 가능성의 증명이다.**

**AC-51** 항상 1.0 을 주는 `SimilarityMetric` 주입 → 모든 결과가 `matched=true`.

**AC-52** `sdk/java-core` 전체에 `dinov2|facebook/|torch|transformers|clip` 이
없다. `check-boundaries.sh` 의 GR-03 과 같은 검사를 테스트로도 건다.

### Spring Boot Starter (AC-60 ~ AC-63) — Phase 4

**AC-60** 최소 설정만으로 컨텍스트를 띄우면 `ImageVerifier` 빈이 존재한다.

**AC-61** 사용자가 `EmbeddingModel` 빈을 정의하면 그것이 쓰이고 기본 HTTP 구현은
등록되지 않는다.

**AC-62** `image-verifier.match-threshold=0.5` → 결과의 `threshold()` 가 0.5.
`timeout`, `parallelism`, `base-url` 도 같은 방식으로 확인.

**AC-63** `match-threshold=1.5` → 컨텍스트 기동 실패(`ConfigurationException`).
런타임에 조용히 이상하게 도는 것보다 못 뜨는 게 낫다.

### 레거시 호환 (AC-70 ~ AC-74)

**AC-70** python-inference 의 `/similarity` 가 예전 요청 형태
(`reviewImage`/`compareImage` 파트)에 `{"similarity": ...}` 로 응답한다.
Phase 2~4 를 지나는 동안 이 테스트는 계속 초록이어야 한다.

**AC-70c (계약)** JDK 스텁 서버는 본문을 원시 바이트로 읽을 뿐 **multipart 파싱을
하지 않는다**. 그래서 손으로 만든 `MultipartBody` 를 실제 `python-multipart` 가
받아들이는지는 스텁으로 증명되지 않는다. `LegacyWireContractTest` 가 그 구멍을
메운다 — 바이트에 `\r\n--boundary\r\n` 를 일부러 심어 보내고, 서버가 계산한
sha256 이 원본과 일치하는지 본다.

기본 빌드에서는 건너뛴다. 켜려면:

```bash
cd sdk && ./gradlew test -Dimageverify.contract.url=http://127.0.0.1:8000/similarity
```

모델 없이도 돌릴 수 있다 — 확인하려는 것은 전송 형식이지 유사도 값이 아니라서,
`/similarity` 와 서명만 같고 고정값을 돌려주는 FastAPI 스텁이면 충분하다.
transformers·GPU·DB 가 없는 환경에서도 이 계약은 검증된다.

**AC-71** `backend=pairwise` 로 두면 `/embed` 를 한 번도 부르지 않는다
(스텁에서 `/embed` 요청 수 0). BC-3 롤백 경로의 실물 검증이다.

**AC-72** 애플리케이션 테스트: 임계값 미달 사진으로 `POST /api/reviews`
→ `422`, 본문 `{"error":"Unprocessable Entity","errorCode":"IMAGE_NOT_MATCHED",
"imageSimilarity":<double>}`. 키 이름과 순서까지 확인한다.

**AC-73** 추론 서버를 끄고 `POST /api/reviews`
→ `503`, `errorCode: "AI_SERVER_UNAVAILABLE"`.

**AC-74** 통과한 리뷰가 저장될 때 `compare_image_url` 이 **최고 점수를 낸 표본
사진의 URL** 이다(대표 사진이 아니다). `image_similarity` 도 결과값 그대로다.

---

## 이 목록에 없는 것

의도적으로 뺐다:

- **정확도(같은 음식/다른 음식 판별율)** — 모델의 성질이지 SDK 의 성질이 아니다.
  `ai/README.md` 의 평가가 그 자리다. 이 SDK 는 0.80 이 좋은 값인지 판단하지 않는다.
- **부하·성능 회귀 테스트** — SPEC §7 의 수치는 **게이트에서 수동 측정**한다.
  자동화된 성능 테스트는 CI 에서 불안정하고, 지금 규모에서는 값보다 잡음이 크다.
- **`/api/reviews` 의 나머지 검증 규칙(별점, 글자수, 자격)** — 이번 마이그레이션이
  건드리지 않는 영역이다. 건드리지 않았음을 확인하는 것이 게이트의 역할이다.
