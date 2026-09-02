#!/usr/bin/env bash
#
# 저장소 수준 아키텍처 경계 검사.
#
# 빌드도 JDK 도 필요 없다 — 문자열 검사만 한다. 그래서 SDK 코드가 한 줄도 없는
# 지금부터 켜 둘 수 있고, 매 Phase 게이트에서 같은 명령으로 돌린다.
#
# 사용:
#   bash sdk/tools/check-boundaries.sh
#
# 종료 코드: 0 = 통과, 1 = 위반 있음
#
# 규칙의 근거는 sdk/docs/ARCHITECTURE.md, 목록은 sdk/docs/GUARDRAILS.md.

set -uo pipefail

cd "$(dirname "$0")/../.." || exit 2

CORE="sdk/java-core"
STARTER="sdk/spring-boot-starter"
APP="backend/Server/src/main/java"

# GR-04(저장소 금지)만 main 소스로 좁힌다. ARCH-R5 의 취지는 "출하되는 SDK 코드가
# 저장소를 모른다"이고, 테스트 코드는 출하되지 않는다. AC-52 처럼 소스 트리를
#직접 훑어야 하는 테스트가 파일 API 를 쓰는 것은 위반이 아니다.
# 나머지 규칙은 테스트 코드까지 전부 검사한다.
CORE_MAIN="sdk/java-core/src/main/java"
STARTER_MAIN="sdk/spring-boot-starter/src/main/java"

RED=$'\033[31m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; DIM=$'\033[2m'; OFF=$'\033[0m'
[ -t 1 ] || { RED=''; GREEN=''; YELLOW=''; DIM=''; OFF=''; }

violations=0
checked=0
skipped=0

# fail_if_found <ID> <설명> <확장자glob> <ERE 패턴> <경로...>
fail_if_found() {
  local id="$1" desc="$2" glob="$3" pattern="$4"; shift 4
  local present=() path
  for path in "$@"; do [ -e "$path" ] && present+=("$path"); done

  if [ ${#present[@]} -eq 0 ]; then
    printf '%s  ~ %s %s%s (대상 없음 — 아직 생성 전)%s\n' "$DIM" "$id" "$desc" "$YELLOW" "$OFF"
    skipped=$((skipped + 1))
    return 0
  fi

  checked=$((checked + 1))
  local hits
  # -i: 대소문자를 무시한다. 여기 패턴은 전부 식별자·패키지 이름이라
  # 대소문자를 바꿔 규칙을 피해 가는 일이 없게 한다.
  hits=$(grep -rEni --include="$glob" -- "$pattern" "${present[@]}" 2>/dev/null)

  if [ -n "$hits" ]; then
    printf '%s  ✗ %s %s%s\n' "$RED" "$id" "$desc" "$OFF"
    printf '%s\n' "$hits" | sed 's/^/      /'
    violations=$((violations + 1))
  else
    printf '%s  ✓ %s%s %s\n' "$GREEN" "$id" "$OFF" "$desc"
  fi
}

echo
echo "아키텍처 경계 검사"
echo "──────────────────────────────────────────────────────────────"

fail_if_found "GR-01" "java-core 에 Spring/jakarta 의존이 없다 (ARCH-R1)" \
  '*.java' '^\s*import\s+(org\.springframework|jakarta)\.' \
  "$CORE"

# SDK 도 group 이 com.reviewticket 이라(패키지 com.reviewticket.sdk.imageverify)
# 접두어 전체를 막을 수는 없다. 애플리케이션 루트인 com.reviewticket.server 만
# 정확히 겨냥한다 — 애플리케이션 클래스는 예외 없이 그 아래에 있다.
fail_if_found "GR-02" "SDK 가 애플리케이션 패키지를 참조하지 않는다 (ARCH-R3)" \
  '*.java' 'com\.reviewticket\.server' \
  "$CORE" "$STARTER"

fail_if_found "GR-03" "java-core 에 특정 모델 이름이 없다 (ARCH-R4)" \
  '*.java' 'dinov2|facebook/|transformers|torch\.|clip-vit' \
  "$CORE"

fail_if_found "GR-04" "SDK main 소스가 파일·저장소 API 를 쓰지 않는다 (ARCH-R5)" \
  '*.java' '(java\.io\.File|java\.nio\.file\.|MultipartFile|ImageStorage|Files\.)' \
  "$CORE_MAIN" "$STARTER_MAIN"

fail_if_found "GR-05" "애플리케이션이 SDK 내부 패키지를 import 하지 않는다 (§3)" \
  '*.java' '^\s*import\s+io\.imageverify\.(core|http)\.' \
  "$APP"

fail_if_found "GR-06" "SDK 에 애플리케이션 예외 이름이 없다 (ARCH-R6)" \
  '*.java' '(ServiceUnavailableException|ImageNotMatchedException|ValidationException|NotFoundException|ForbiddenException|ConflictException|UnauthorizedException)' \
  "$CORE" "$STARTER"

fail_if_found "GR-07" "java-core 가 starter 를 참조하지 않는다 (ARCH-R2)" \
  '*.java' 'io\.imageverify\.spring' \
  "$CORE"

fail_if_found "GR-08" "java-core 빌드에 외부 의존성이 없다 (SPEC D-2)" \
  'build.gradle' '^\s*(implementation|api|runtimeOnly|compileOnly)\s+' \
  "$CORE/build.gradle"

echo "──────────────────────────────────────────────────────────────"
if [ "$violations" -gt 0 ]; then
  printf '%s위반 %d건. 게이트 실패 — 진행하지 않는다.%s\n\n' "$RED" "$violations" "$OFF"
  exit 1
fi
printf '%s통과%s (검사 %d건, 대상 없어 건너뜀 %d건)\n\n' "$GREEN" "$OFF" "$checked" "$skipped"
exit 0
