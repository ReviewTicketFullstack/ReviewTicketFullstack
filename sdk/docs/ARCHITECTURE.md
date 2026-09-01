<title>ARCHITECTURE — 의존성 경계</title>

# ARCHITECTURE — 의존성 경계

이 문서의 규칙은 의견이 아니라 **검사되는 제약**이다. 각 규칙에는 이를 강제하는
가드레일 ID 가 붙어 있다(→ `GUARDRAILS.md`). 검사가 없는 규칙은 규칙이 아니다.

---

## 1. 모듈

```
sdk/
  java-core/            imageverify-core                외부 의존성 0개, Spring 없음
  spring-boot-starter/  imageverify-spring-boot-starter  java-core + Spring Boot
  python-inference/     추론 서버 (FastAPI)              별도 프로세스, 별도 배포 단위
```

`backend/Server` 는 이 셋 중 **starter 만** 의존한다.

## 2. 허용된 의존 방향

```
  backend/Server (애플리케이션)
        │
        ▼
  spring-boot-starter
        │
        ▼
  java-core  ──── HTTP ────▶  python-inference
```

| From → To | 허용 | 강제 |
|---|---|---|
| 애플리케이션 → starter | ✅ | — |
| 애플리케이션 → java-core `api`/`spi` | ✅ | — |
| 애플리케이션 → java-core `core`/`http` | ❌ 내부 구현이다 | GR-05 |
| starter → java-core | ✅ | — |
| java-core → starter | ❌ | GR-07 |
| java-core → Spring / jakarta | ❌ | GR-01 |
| SDK(어느 모듈이든) → 애플리케이션 | ❌ | GR-02 |
| java-core → python-inference | HTTP 만. 코드 의존 없음 | GR-03 |

**역방향 화살표는 하나도 없다.** 애플리케이션을 지워도 SDK 는 그대로 빌드된다 —
이게 "재사용 가능"의 실질적 정의다.

## 3. 패키지 구조와 가시성

```
com.reviewticket.sdk.imageverify.api      공개. 소비자가 import 하는 유일한 곳
                        ImageVerifier, ReferenceImage, VerificationResult, Score,
                        VerifierConfig, 예외 4종
com.reviewticket.sdk.imageverify.spi      공개 확장점. 직접 구현해 갈아끼울 때만 쓴다
                        EmbeddingModel, PairwiseModel, SimilarityMetric, EmbeddingCache
com.reviewticket.sdk.imageverify.core     내부. 기본 구현
                        DefaultImageVerifier, CosineSimilarity, LruEmbeddingCache
com.reviewticket.sdk.imageverify.http     내부. 추론 서버 어댑터
                        HttpEmbeddingModel, HttpPairwiseModel
com.reviewticket.sdk.imageverify.spring   starter 모듈. 자동 구성과 프로퍼티
```

`core` 와 `http` 는 SemVer 상 내부다. 여기 클래스를 애플리케이션이 직접 import
하면 가드레일이 실패한다(GR-05).

---

## 4. 규칙

### ARCH-R1 — java-core 는 Spring 을 모른다
`org.springframework.*`, `jakarta.*` import 가 `sdk/java-core` 아래 단 한 줄도
없어야 한다. 테스트 코드도 포함이다.
→ **GR-01**

이유: 상용 SDK 가 Spring 을 강제하면 Quarkus·Micronaut·순수 Java 사용자가
못 쓴다. Spring 은 편의 계층이지 요구사항이 아니다.

HTTP 클라이언트는 `RestClient` 대신 JDK `java.net.http.HttpClient` 를 쓴다.
현행 `SimpleClientHttpRequestFactory` 의 connect/read 타임아웃은
`HttpClient.connectTimeout` + `HttpRequest.timeout` 으로 1:1 대응한다.

### ARCH-R2 — starter 는 java-core 에만 의존한다
starter 에는 로직이 없다. 프로퍼티 바인딩과 빈 등록뿐이다. 판정 로직이
starter 에 생기면 Spring 없는 사용자가 그 기능을 못 쓴다.
→ **GR-07** (역방향 금지), 코드 리뷰 (로직 없음)

모든 빈은 `@ConditionalOnMissingBean` 으로 등록한다. 사용자가 자기
`EmbeddingModel` 을 올리면 우리 기본값이 조용히 물러난다.

### ARCH-R3 — 애플리케이션 코드는 SDK 를 향해서만 흐른다
`sdk/**` 어디에도 `com.reviewticket.server` 문자열이 없어야 한다. import,
클래스 이름, 주석의 예시까지 전부다.
→ **GR-02**

SDK 의 group 도 `com.reviewticket` 이라(패키지 `com.reviewticket.sdk.imageverify`)
접두어 전체를 막을 수는 없다. 그래서 **애플리케이션 루트인
`com.reviewticket.server` 를 정확히 겨냥한다** — 애플리케이션 클래스는 예외 없이
그 아래에 있으므로 정밀도 손실이 없다.

이름이 겹친다는 사실이 경계를 흐리지는 않는다. `com.reviewticket.sdk.*` 는
ReviewTicket 을 모르는 코드이고, `com.reviewticket.server.*` 는 ReviewTicket
그 자체다. 둘 사이에 화살표는 한 방향으로만 존재한다.

`ReviewService` 는 `ImageVerifier` 를 부른다. `ImageVerifier` 는 `ReviewService`
가 존재하는지 모른다.

### ARCH-R4 — AI 모델은 교체 가능하다
`sdk/java-core` 에 `dinov2`, `facebook/`, `torch`, `transformers`, `clip`
문자열이 없어야 한다 — 상수, 기본값, 클래스 이름, 주석 전부 포함.
→ **GR-03**

모델 식별자는 우리가 아는 값이 아니라 **추론 서버가 응답 헤더로 알려 주는 값**
(`X-Model-Id`)이다. java-core 는 그걸 캐시 키에 문자열로 끼워 넣을 뿐 해석하지
않는다. 그래서 모델을 바꾸는 일은 java-core 재컴파일 없이 python 쪽 교체와
캐시 자동 무효화로 끝난다.

DINOv2 라는 이름이 등장해도 되는 곳은 딱 두 군데다 — `sdk/python-inference/`
와 문서(`.md`).

### ARCH-R5 — 저장소는 SDK 밖이다
`sdk/**` 에 `java.io.File`, `java.nio.file.*`, `MultipartFile`, `ImageStorage`,
`Files.` 가 없어야 한다.
→ **GR-04**

SDK 는 바이트를 어디서 가져오는지 몰라야 한다. 그 지식은 전부
`ReferenceImage` 의 `Supplier<byte[]>` 안에 갇힌다 — 그 람다는 애플리케이션이
만들고 애플리케이션이 캡처한다.

이 규칙이 지켜지면 같은 SDK 가 S3, DB BLOB, 메모리, 원격 URL 어디서 오는
바이트든 그대로 처리한다.

### ARCH-R6 — 애플리케이션 예외는 SDK 로 새지 않는다
`sdk/**` 에 `ServiceUnavailableException`, `ImageNotMatchedException`,
`ValidationException`, `NotFoundException` 등 애플리케이션 예외 이름이 없어야 한다.
→ **GR-06**

번역은 애플리케이션 경계에서 딱 한 번 일어난다:

```
InferenceUnavailableException  →  ServiceUnavailableException("AI_SERVER_UNAVAILABLE")
!result.matched()              →  ImageNotMatchedException(result.similarity())
```

이 두 줄이 SDK 와 ReviewTicket 이 만나는 전부여야 한다.

### ARCH-R7 — 동시성은 SDK 소유다
현재 `ReviewService` 가 들고 있는 `Executors.newFixedThreadPool(5)` 와
`@PreDestroy` 는 SDK 로 옮긴다. 스레드풀 크기는 `parallelism` 설정이고,
수명은 SDK 가 관리한다.

애플리케이션에는 `CompletableFuture` 도 `CompletionException` 언랩도 남지
않는다.

### ARCH-R8 — 빌드 결합은 composite build 로
`sdk/` 는 자체 Gradle 루트다. `backend/Server/settings.gradle` 이
`includeBuild('../../sdk')` 로 끌어다 쓴다.

Maven 저장소도, 버전 올리기도, 로컬 publish 도 필요 없다. 그러면서도 SDK 는
독립적으로 빌드·테스트되고 나중에 그대로 publish 할 수 있다.
`./gradlew build` 한 줄로 애플리케이션을 빌드하는 현재 흐름은 유지된다.

---

## 5. 이 경계가 사는 방식

규칙은 잊힌다. 그래서 세 겹으로 검사한다.

| 겹 | 무엇 | 언제 |
|---|---|---|
| 1 | `sdk/tools/check-boundaries.sh` — 문자열 기반, 빌드 불필요 | 지금부터, 매 단계 게이트 |
| 2 | CI 잡 `sdk-boundaries` | 모든 PR |
| 3 | ArchUnit 테스트 (Phase 4) | `./gradlew test` |

1번은 **오늘 이미 돈다**. 코드가 한 줄도 없을 때부터 켜 두는 것이 요점이다 —
나중에 켜면 이미 어겨진 것을 발견하게 된다.
