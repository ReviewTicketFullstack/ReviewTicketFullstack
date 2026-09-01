<title>GUARDRAILS — 경계 강제</title>

# GUARDRAILS — 경계 강제

`ARCHITECTURE.md` 의 규칙을 **자동으로 실패시키는** 장치들. 문서로만 존재하는
규칙은 6개월 뒤에 없다.

## 실행

```bash
bash sdk/tools/check-boundaries.sh
```

빌드도 JDK 도 필요 없다. 종료 코드 0 = 통과, 1 = 위반.
아직 없는 대상(`sdk/java-core` 등)은 `~ (대상 없음)` 으로 표시하고 건너뛴다 —
그래서 코드가 없는 지금도 돌아가고, 모듈이 생기는 즉시 저절로 검사 대상이 된다.

## 규칙 목록

| ID | 막는 것 | 규칙 | 검사 방식 |
|---|---|---|---|
| GR-01 | java-core 에 Spring 이 들어옴 | ARCH-R1 | `sdk/java-core/**/*.java` 에 `import org.springframework.` / `import jakarta.` |
| GR-02 | SDK 가 ReviewTicket 비즈니스 패키지를 import | ARCH-R3 | `sdk/**/*.java` 에 `com.reviewticket.server` |
| GR-03 | SDK 가 DINOv2 에 직접 의존 | ARCH-R4 | `sdk/java-core/**/*.java` 에 `dinov2` / `facebook/` / `transformers` / `torch.` / `clip-vit` |
| GR-04 | SDK 가 애플리케이션 저장소에 의존 | ARCH-R5 | `sdk/**/*.java` 에 `java.io.File` / `java.nio.file.` / `MultipartFile` / `ImageStorage` / `Files.` |
| GR-05 | 애플리케이션이 SDK 내부 구현을 import | 패키지 가시성 | `backend/Server/**/*.java` 에 `import com.reviewticket.sdk.imageverify.core.` / `com.reviewticket.sdk.imageverify.http.` |
| GR-06 | 애플리케이션 예외가 SDK 로 샘 | ARCH-R6 | `sdk/**/*.java` 에 `ServiceUnavailableException` 등 7종 |
| GR-07 | java-core → starter 역방향 의존 | ARCH-R2 | `sdk/java-core/**/*.java` 에 `com.reviewticket.sdk.imageverify.spring` |
| GR-08 | java-core 에 외부 라이브러리 유입 | SPEC D-2 | `sdk/java-core/build.gradle` 에 `implementation`/`api`/`runtimeOnly`/`compileOnly` 줄 |

검사는 전부 **대소문자 무시**다. `DINOv2` 를 `dinoV2` 로 써서 피해 갈 수 없다.

> **GR-02 가 `com.reviewticket` 전체가 아니라 `com.reviewticket.server` 인 이유** —
> SDK 의 group 도 `com.reviewticket` 이라(패키지 `com.reviewticket.sdk.imageverify`)
> 접두어 전체를 막으면 SDK 자기 자신이 걸린다. 애플리케이션 클래스는 예외 없이
> `com.reviewticket.server` 아래에 있으므로 이렇게 좁혀도 놓치는 것이 없다.

## 검사기 자체의 검증

가드레일에서 가장 흔한 실패는 "규칙이 아무것도 안 잡고 있는데 초록"이다.
검사기를 고칠 때마다 일부러 위반하는 파일을 만들어 **빨강이 뜨는지** 확인한다:

```bash
D=sdk/java-core/src/main/java/com/reviewticket/sdk/imageverify/core
cat > "$D/Bad.java" <<'EOF'
package com.reviewticket.sdk.imageverify.core;
import org.springframework.stereotype.Component;
import com.reviewticket.server.image.ImageStorage;
import java.nio.file.Files;
class Bad { String m = "facebook/DINOv2-base"; ServiceUnavailableException e; }
EOF
bash sdk/tools/check-boundaries.sh   # GR-01,02,03,04,06 이 실패해야 한다
rm "$D/Bad.java"
```

Phase 0 과 Phase 1 에서 각각 실행해 5개 규칙이 모두 발화하는 것을 확인했다.
특히 Phase 1 에서는 `com.reviewticket.sdk.*` 인 정상 코드가 GR-02 에 걸리지
않으면서 `com.reviewticket.server` 참조만 잡히는지를 함께 확인했다.

## 세 겹 방어

| 겹 | 장치 | 언제 도는가 | 상태 |
|---|---|---|---|
| 1 | `check-boundaries.sh` | 로컬, 매 Phase 게이트 | **가동 중** |
| 2 | CI 잡 `sdk-boundaries` | 모든 PR | Phase 1 에서 `.github/workflows/ci.yml` 에 추가 |
| 3 | ArchUnit 테스트 | `./gradlew test` | Phase 4 |

1번만으로도 규칙은 강제된다. 2번은 사람이 로컬 실행을 잊는 경우를, 3번은
문자열 검사가 놓치는 구조적 위반(예: 리플렉션 경유 의존)을 잡는다.

### CI 추가분 (Phase 1에서 넣을 것)

```yaml
  sdk-boundaries:
    name: SDK Architecture Boundaries
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: bash sdk/tools/check-boundaries.sh
```

기존 `frontend-build`·`backend-build` 와 나란히 둔다. 빌드가 필요 없어 몇 초면
끝나므로, 실패를 가장 먼저 알려 주는 잡이 된다.

---

## 자동화되지 않는 점검 — 사람이 봐야 하는 것

문자열로는 잡히지 않지만 어기면 SDK 가 조용히 틀리는 것들. **Phase 3 와 Phase 5
게이트의 체크리스트 항목**이다.

**MG-01. 캐시 키 안정성 (SPEC §4.2).**
`ReferenceImage.key` 뒤의 바이트가 같은 key 를 유지한 채 바뀔 수 있는 경로가
새로 생기지 않았는가. 현재는 `ImageStorage.save()` 가 항상 새 UUID 를 만들어
안전하다. 파일을 덮어쓰는 코드가 생기면 캐시가 낡은 임베딩을 계속 쓴다.
→ Phase 3 게이트에서 `ImageStorage` 와 `Menu.applyEdit` 을 눈으로 확인한다.

**MG-02. starter 에 로직이 없는가 (ARCH-R2).**
자동 구성 클래스에 `if` 가 늘어나기 시작하면 Spring 없는 사용자가 못 쓰는
기능이 생긴 것이다. 판정에 관한 분기는 전부 java-core 에 있어야 한다.

**MG-03. 애플리케이션 → SDK 번역 지점이 두 곳뿐인가 (ARCH-R6).**
`InferenceUnavailableException → ServiceUnavailableException` 과
`!matched → ImageNotMatchedException`. 세 번째 번역이 생겼다면 SDK 가 도메인을
알기 시작했다는 신호다.

**MG-04. 범위 이탈.**
이번 마이그레이션은 리뷰 검증 경로만 건드린다. diff 에 `auth/`, `order/`,
`store/`, `frontend/` 파일이 있으면 이유를 설명할 수 있어야 한다.
→ 각 게이트에서 `git diff --stat` 로 확인한다.

**MG-05. `/similarity` 존속 (BC-5).**
Phase 2~5 어느 시점에도 `sdk/python-inference/main.py` 에서 `/similarity`
핸들러가 사라지지 않았는가. 롤백 경로다. 삭제는 Phase 5 검증 완료 + 명시적
승인 뒤에만 한다.
