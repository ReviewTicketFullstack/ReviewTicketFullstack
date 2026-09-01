<title>이미지 검증 SDK</title>

# 이미지 검증 SDK

기준 이미지 여러 장과 후보 이미지 한 장을 받아, 가장 잘 맞는 기준 이미지와
유사도, 임계값 통과 여부를 돌려주는 재사용 가능한 라이브러리.

```java
VerificationResult result = imageVerifier.verify(references, candidateBytes);
if (result.matched()) { /* ... */ }
```

FastAPI, HTTP, 임베딩 모델, 유사도 계산, 캐싱은 전부 이 한 줄 뒤에 숨는다.

**현재 상태: Phase 0 (하네스 작성 완료). 구현 코드 없음.**

## 문서

| 파일 | 내용 |
|---|---|
| [docs/SPEC.md](docs/SPEC.md) | 공개 API, 입출력 계약, 오류 동작, 설정, 캐싱, 성능, 하위 호환 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 모듈 경계와 의존 방향 규칙 (ARCH-R1~R8) |
| [docs/ACCEPTANCE.md](docs/ACCEPTANCE.md) | 인수 기준 (AC-01~74), Phase 별 적용 범위 |
| [docs/GUARDRAILS.md](docs/GUARDRAILS.md) | 경계를 강제하는 검사 (GR-01~08) 와 수동 점검 (MG-01~05) |
| [docs/PLAN.md](docs/PLAN.md) | 5단계 마이그레이션과 각 단계의 게이트 |

읽는 순서: SPEC → ARCHITECTURE → PLAN.

## 경계 검사

```bash
bash tools/check-boundaries.sh
```

빌드가 필요 없다. 모듈이 아직 없는 지금도 돌아가며, 매 Phase 게이트에서 같은
명령으로 확인한다.

## 예정 구조

```
sdk/
  java-core/            외부 의존성 0개. Spring 없음
  spring-boot-starter/  자동 구성만. 로직 없음
  python-inference/     FastAPI 추론 서버 (현 ai/server 이전 예정)
  docs/                 이 하네스
  tools/                경계 검사기
```

의존 방향은 한쪽으로만 흐른다:
`애플리케이션 → starter → java-core → (HTTP) → python-inference`.
역방향 화살표는 없다 — 애플리케이션을 지워도 SDK 는 그대로 빌드된다.
