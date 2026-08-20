/**
 * 서버가 보낸 errorCode 를 화면 문구로 옮기는 표.
 *
 * 서버는 errorCode 만 보내고 문구는 프론트가 정한다(ApiExceptionHandler 규약).
 * 여기 없는 코드는 HTTP 상태로 만든 일반 안내가 대신 나간다 —
 * 상태 코드는 도메인을 모르므로, 코드별 문구가 필요하면 반드시 이 표에 넣는다.
 *
 * 화면마다 다르게 말해야 하는 코드는 여기 두지 않고 그 화면에서 고른다
 * (ReviewModal 의 IMAGE_NOT_MATCHED 처럼). 이 표는 어느 화면에서 떠도
 * 말이 되는 문구만 담는다.
 */
const MESSAGE_BY_ERROR_CODE: Record<string, string> = {
  // 주문·티켓
  NO_TICKETS_LEFT: "티켓수량이 부족합니다.",
  REVIEW_EVENT_NOT_AVAILABLE: "이벤트 대상이 아닌 메뉴입니다.",
  MENU_STORE_MISMATCH:
    "메뉴 정보가 올바르지 않습니다. 새로고침 후 다시 시도해 주세요.",

  // 계정 — 이메일·이름
  EMAIL_TAKEN: "이미 가입된 이메일입니다.",
  NAME_TAKEN: "이미 사용 중인 이름입니다.",
  ALREADY_IN_USE: "이미 사용 중인 값입니다. 다른 값으로 시도해 주세요.",
  EMAIL_TOO_LONG: "이메일이 너무 깁니다.",
  EMAIL_DOMAIN_INVALID: "메일을 받을 수 없는 이메일 주소입니다.",
  NO_EXISTING_EMAIL: "가입되지 않은 이메일입니다.",
  NAME_REQUIRED: "이름을 입력해 주세요.",
  NAME_TOO_LONG: "이름은 32자 이하여야 합니다.",

  // 계정 — 비밀번호 (PasswordPolicy: 6~14자)
  PASSWORD_MISMATCH: "비밀번호가 서로 다릅니다.",
  PASSWORD_TOO_SHORT: "비밀번호는 6자 이상이어야 합니다.",
  PASSWORD_TOO_LONG: "비밀번호는 14자 이하여야 합니다.",
  PASSWORD_MISSING_UPPER: "비밀번호에 대문자가 필요합니다.",
  PASSWORD_MISSING_LOWER: "비밀번호에 소문자가 필요합니다.",
  PASSWORD_MISSING_DIGIT: "비밀번호에 숫자가 필요합니다.",
  PASSWORD_MISSING_SPECIAL: "비밀번호에 특수문자가 필요합니다.",
  PASSWORD_INVALID_CHAR: "비밀번호에 쓸 수 없는 문자가 있습니다.",

  // 인증·권한
  LOGIN_FAILED: "이메일 또는 비밀번호가 올바르지 않습니다.",
  UNAUTHORIZED: "로그인이 필요합니다. 다시 로그인해 주세요.",
  ROLE_MISMATCH: "계정의 역할에 맞지 않는 접근입니다.",
  NOT_OWNER: "사장님 계정으로 로그인해 주세요.",
  NOT_CUSTOMER: "고객 계정으로 로그인해 주세요.",
  NOT_ORDER_OWNER: "본인의 주문이 아닙니다.",
  VERIFY_TOKEN_INVALID: "인증 링크가 올바르지 않습니다.",
  VERIFY_TOKEN_EXPIRED: "인증 링크가 만료되었습니다.",
  RESET_TOKEN_INVALID: "재설정 링크가 올바르지 않습니다.",
  RESET_TOKEN_EXPIRED: "재설정 링크가 만료되었거나 이미 사용되었습니다.",
  RESEND_COOLDOWN: "잠시 후 다시 요청해 주세요.",

  // 리뷰
  INVALID_RATING: "별점을 선택해 주세요.",
  CONTENT_TOO_SHORT: "후기가 너무 짧습니다.",
  CONTENT_TOO_LONG: "후기가 너무 깁니다.",
  IMAGE_REQUIRED: "사진을 첨부해 주세요.",
  REVIEW_ALREADY_EXISTS: "이미 후기를 작성한 주문입니다.",
  REVIEW_EVENT_NOT_APPLIED: "이벤트에 참여하지 않은 주문입니다.",
  REVIEW_PERIOD_EXPIRED: "후기 작성 마감 시각이 지났습니다.",
  MENU_SAMPLE_MISSING: "이 메뉴에 등록된 표본 사진이 없습니다.",

  // 가게·메뉴
  STORE_NOT_FOUND: "가게 정보를 찾을 수 없습니다.",
  MENU_NOT_FOUND: "메뉴 정보를 찾을 수 없습니다.",
  ORDER_NOT_FOUND: "주문 정보를 찾을 수 없습니다.",
  TOO_MANY_SAMPLE_IMAGES: "표본 사진이 너무 많습니다.",
  SAMPLE_IMAGE_REQUIRED: "표본 사진을 한 장 이상 등록해야 합니다.",

  // 업로드
  FILE_REQUIRED: "파일을 선택해 주세요.",
  FILE_TOO_LARGE: "파일 용량이 너무 큽니다.",
  UNSUPPORTED_IMAGE_TYPE: "jpeg, png 형식만 올릴 수 있습니다.",
  IMAGE_TOO_SMALL: "사진 해상도가 너무 낮습니다.",

  // 외부 서비스
  AI_SERVER_UNAVAILABLE:
    "사진 확인 서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.",
};

/** 아는 코드면 문구를, 모르는 코드(또는 코드 없음)면 undefined 를 준다. */
export function messageForErrorCode(errorCode?: string) {
  if (!errorCode) return undefined;
  return MESSAGE_BY_ERROR_CODE[errorCode];
}
