<title>PLAN — 마이그레이션 단계</title>

# PLAN — 마이그레이션 단계

각 Phase 는 **되돌릴 수 있는 단위**다. 끝에 게이트가 있고, 게이트가 빨강이면
다음 Phase 로 자동 진행하지 않는다 — 실패를 보고하고 멈춘다.

## 모든 게이트에서 공통으로 도는 것

```bash
# 1. 경계
bash sdk/tools/check-boundaries.sh

# 2. SDK 빌드 + 테스트
cd sdk && ./gradlew build

# 3. 애플리케이션 빌드 + 테스트
cd backend/Server && ./gradlew build

# 4. 프론트엔드 (건드리지 않았음을 확인)
cd frontend && npm run build

# 5. 범위 이탈 확인 (MG-04)
git diff --stat
```

여기에 Phase 별 인수 테스트(`ACCEPTANCE.md` 의 해당 열)와 수동 점검이 더해진다.

**게이트 실패 시 규칙 — 예외 없음:**
1. 멈춘다. 다음 Phase 를 시작하지 않는다.
2. 무엇이 왜 실패했는지 그대로 보고한다. 실패를 우회하는 수정을 먼저 하지 않는다.
3. 게이트를 느슨하게 고쳐서 통과시키지 않는다. 게이트가 틀렸다고 판단되면
   그 판단 자체를 보고하고 승인을 받는다.

---

## Phase 0 — 하네스 (지금)

SPEC, ARCHITECTURE, ACCEPTANCE, GUARDRAILS, PLAN, 그리고 동작하는
`check-boundaries.sh`.

**게이트:** 검사기가 실행되고, 일부러 만든 위반 파일에 빨강이 뜬다. ✅ 확인 완료.
**미결:** 결정 D-1(패키지 이름). **Phase 1 착수 전에 확정한다.**

---

## Phase 1 — 레거시 경로 그대로 `ImageVerifier` 추출

> 목표: **동작이 완전히 동일한 상태에서** 코드의 위치만 바꾼다.
> 새 기능 없음, 새 엔드포인트 없음, 성능 변화 없음.

**만드는 것**
- `sdk/settings.gradle`, `sdk/java-core` 모듈 (외부 의존성 0개)
- `api` 패키지: `ImageVerifier`, `ReferenceImage`, `VerificationResult`, `Score`,
  `VerifierConfig`, 예외 4종
- `spi` 패키지: `PairwiseModel`
- `http`: `HttpPairwiseModel` — 현행 `ImageSimilarityClient` 를 JDK `HttpClient`
  로 옮긴 것. 요청 형태(`reviewImage`/`compareImage`)와 응답 파싱은 그대로
- `core`: `DefaultImageVerifier` — 현행 `measureBest` 의 팬아웃·최댓값·임계값
  비교를 그대로 옮긴 것. 스레드풀 소유권도 여기로(ARCH-R7)
- `backend/Server/settings.gradle` 에 `includeBuild('../../sdk')`
- 애플리케이션 쪽 얇은 어댑터: 예외 번역 2줄
- CI 잡 `sdk-boundaries` 추가

**건드리는 애플리케이션 파일**
`ReviewService`(AI 관련 부분만), `settings.gradle`, `build.gradle`, 그리고
SDK 빈을 조립하는 설정 클래스 1개(신규). `ReviewTransaction`·`ImageStorage`·
`ImageResizer`·컨트롤러·예외 핸들러는 **손대지 않는다.**

`ImageSimilarityClient` 는 **지우지 않는다.** 쓰이지 않는 채로 남겨 둔다 —
롤백이 `ReviewService` 한 곳을 되돌리는 것으로 끝나게 하기 위해서다.
제거는 Phase 5 에서 한다.

**게이트 (공통 + 아래)**
- 인수 테스트 P1 열 전체 (AC-01~15, 20~27, 52, 70) — ✅ 32건 통과
- 계약 테스트 AC-70c: 실제 `python-multipart` 가 우리 multipart 본문을 파싱하는지.
  모델도 DB 도 필요 없다(§AC-70c) — ✅ 통과, 바이트 sha256 일치 확인
- 수동: 리뷰 1건을 실제로 등록해 통과·거부 양쪽을 확인. 응답 본문이 이전과 동일
  — ⏳ **미실시.** MySQL 과 transformers 가 필요하다(아래)
- 수동: `git diff` 에 위 파일들 외의 변경이 없는지 — ✅ `ai/`·`frontend/` 0줄

**종단 확인에 필요한 것 (아직 안 된 유일한 항목)**

| 필요한 것 | 왜 | 없으면 못 보는 것 |
|---|---|---|
| MySQL (`localhost:21096`) | 주문·리뷰 행이 있어야 `/api/reviews` 가 돈다 | 422/503 응답의 실제 형태, 저장되는 `compare_image_url` |
| `pip install transformers` + 모델 | DINOv2 임베딩 | 실제 유사도 값, 0.80 문턱값 근처 동작 |

전송 형식은 위 계약 테스트가 이미 덮었으므로, 남은 것은 **판정 값과 응답 형태**뿐이다.

**롤백:** 커밋 되돌리기. python 쪽은 한 줄도 안 바뀌었다.

---

## Phase 2 — `/embed` 추가, 임베딩과 유사도 분리

> 목표: 임베딩을 값으로 꺼낸다. 캐싱은 아직 하지 않는다.

**만드는 것**
- `sdk/python-inference/` (현 `ai/server/` 이전) 에 `POST /embed` **추가**.
  `/similarity` 는 그대로 둔다(BC-5). 응답은 float32 바이너리 + 헤더
  (SPEC §8.2)
- `spi.EmbeddingModel`, `spi.SimilarityMetric`
- `core.CosineSimilarity`
- `http.HttpEmbeddingModel`
- `backend` 설정 스위치: `pairwise` | `embedding`. **기본값은 아직 `pairwise`**
- `ai/README.md` 에 이전 위치 안내를 남긴다

**게이트 (공통 + 아래)**
- 인수 테스트 P2 열 (P1 전체 + AC-30, 31, 32, 71)
- **AC-32 가 이 Phase 의 핵심이다** — 두 백엔드의 `similarity` 차이 ≤ 1e-5,
  `matched`/`matchedKey` 완전 일치. 통과 못 하면 진행하지 않는다(BC-2)
- **AC-30 계약 테스트를 실제 python 을 띄워 돌린다.** torch 코사인 vs Java 코사인
- 수동: 실제 리뷰 사진 20쌍을 양쪽 백엔드로 돌려 0.80 근처에서 판정이 뒤집히는
  사례가 0건인지 확인. 숫자를 기록해 남긴다

**롤백:** 설정 한 줄(`backend=pairwise`). 배포 불필요.

---

## Phase 3 — 임베딩 캐시 + 배치 추론

> 목표: SPEC §7 의 "웜 캐시 = 임베딩 1회, 왕복 1회"를 실제로 달성한다.

**만드는 것**
- `spi.EmbeddingCache`, `core.LruEmbeddingCache` (직접 구현, 의존성 0개)
- `DefaultImageVerifier` 에 캐시 조회 → 미적중분만 1회 배치 요청 → 점수 계산
- `ImageVerifier.warmUp`
- 기본 백엔드를 `embedding` 으로 전환
- python: `/embed` 가 배치를 실제로 배치로 처리하도록 정리
  (`torch.set_num_threads(1)` 설정이 배치에 여전히 맞는지 재확인)

**게이트 (공통 + 아래)**
- 인수 테스트 P3 열 (AC-40~48, 50, 51 추가)
- **수동 성능 측정:** 표본 5장 기준, 콜드/웜 각각의 소요 시간을 실측해
  SPEC §7 표에 기록. 웜 p95 ≤ 1.0초
- **MG-01 수동 점검:** 캐시 키 안정성. `ImageStorage`, `Menu.applyEdit` 확인
- 수동: 사장이 메뉴 표본 사진을 바꾼 직후 첫 리뷰가 **새 사진** 기준으로
  판정되는지 실제로 확인 (캐시 오염 여부)

**롤백:** `cache.enabled=false` (AC-48 이 결과 동일성을 보장) 또는
`backend=pairwise`.

---

## Phase 4 — Spring Boot Starter

> 목표: `ImageVerifier` 를 설정 몇 줄로 쓰는 라이브러리로 만든다.

**만드는 것**
- `sdk/spring-boot-starter` 모듈
- `ImageVerifierProperties` (`image-verifier.*`), `ImageVerifierAutoConfiguration`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 모든 빈에 `@ConditionalOnMissingBean`
- ArchUnit 테스트(가드레일 3겹째)
- `sdk/README.md` 에 소비자용 사용법

**게이트 (공통 + 아래)**
- 인수 테스트 P4 열 (AC-60~63 추가)
- ArchUnit 통과
- 수동: starter 를 쓰지 않는 순수 Java `main()` 으로 `ImageVerifier` 를 손으로
  조립해 동작시킨다. **Spring 없이 도는 것을 눈으로 확인하는 절차다** —
  ARCH-R1 이 문자열 검사만으로는 증명되지 않는다

**롤백:** 애플리케이션은 아직 starter 를 쓰지 않는다(Phase 5 에서 전환).
모듈 추가만이라 영향 범위가 없다.

---

## Phase 5 — ReviewTicket 전면 전환

> 목표: 애플리케이션이 SDK 만 보게 한다. 중복 설정·중복 코드를 정리한다.

**만드는 것 / 지우는 것**
- `backend/Server` 가 starter 를 의존하도록 전환
- `reviewticket.ai.*` 제거, `image-verifier.*` 로 일원화 (BC-4 유예 종료)
- 애플리케이션에 남은 AI 관련 잔재 정리
- `StoreService.updateMyMenu`/`createStore` 에서 `warmUp` 호출 (선택)

**게이트 (공통 + 아래)**
- 인수 테스트 P5 열 전체 (AC-72, 73, 74 추가)
- **AC-72/73/74 가 이 Phase 의 핵심이다** — 외부 계약이 그대로인지(BC-1)
- 수동 종단 확인: 브라우저에서 리뷰 등록 → 통과 / 거부(일치율 표시) /
  AI 서버 끔(503 안내) 세 경로
- 수동: 프론트엔드 코드가 한 줄도 바뀌지 않았음을 `git diff` 로 확인
- **MG-03 점검:** 번역 지점이 두 곳뿐인가

**롤백:** 커밋 되돌리기. `/similarity` 와 pairwise 경로가 아직 살아 있다.

---

## Phase 5 이후 — 이번 범위 밖

승인 없이 진행하지 않는다.

- `/similarity` 엔드포인트 제거 (BC-5 해제)
- 아티팩트 좌표 확정, 라이선스, Maven Central publish (결정 D-3)
- 별도 저장소 분리
- 캐시 통계·메트릭 API

---

## 하지 않는 것

이번 마이그레이션에서 **의도적으로 건드리지 않는다.** diff 에 나타나면
게이트에서 지적한다(MG-04).

- 인증·주문·가게·업로드 기능
- `ImageResizer` 의 리사이즈 정책, 최소 긴 변 규칙
- `ReviewTransaction` 의 트랜잭션 경계 설계
- 프론트엔드
- 임계값 0.80 의 적정성 (모델 평가의 영역)
- Redis / Kafka / Kubernetes — 도입하지 않는다
