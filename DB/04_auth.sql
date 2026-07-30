-- ReviewTicket — 회원/인증 스키마 (BE-1.1 ~ BE-1.6)
--
-- 범위: 회원가입, 이메일 인증, 로그인, 닉네임/비밀번호 변경까지.
-- 주문·티켓·메뉴 표는 아직 없다. 그 기능을 붙일 때 05_*.sql 로 추가한다.
--
-- 실행:
--   Get-Content "C:\dev\ReviewTicket\BackEnd\DB\04_auth.sql" | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u reviewticket -p

USE reviewticket;


-- ============================================================
-- 1. users — 회원
-- ============================================================
-- id 는 화면에 노출하지 않는 내부 회원 번호다. AUTO_INCREMENT 라
-- 가입 순서대로 1, 2, 3... 이 매겨진다. 앞으로 주문·리뷰·티켓이
-- 이 값을 외래키로 참조한다.
--
-- display_name 에 고객 닉네임과 사장 가게 이름을 함께 담는다.
-- 두 이름이 서로 겹치는 것도 막기로 했으므로 한 컬럼 한 UNIQUE 로
-- 끝나고, 중복 검사 API 도 하나면 된다. 화면 좌측 상단에 띄우는
-- 이름이 역할과 무관하게 이 필드 하나라는 점도 이득이다.
--
-- 대소문자: utf8mb4_0900_ai_ci 는 대소문자를 구분하지 않는다.
-- 그래서 'Abc@x.com' 으로 가입한 뒤 'abc@x.com' 으로 다시 가입할 수
-- 없고, 닉네임 'Bryan' 과 'bryan' 도 중복으로 걸린다. 의도한 동작이다.
--
-- token_version: JWT 를 즉시 무효화하는 장치. 비밀번호를 바꿀 때
-- 서버가 이 값을 +1 하고, 들어온 토큰의 tv 클레임과 다르면 거부한다.
-- 이게 없으면 비밀번호를 바꿔도 탈취된 옛 토큰이 만료까지 살아있다.

CREATE TABLE IF NOT EXISTS users (
  id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '내부 회원 번호. 화면에 노출하지 않는다',
  email              VARCHAR(190) NOT NULL COMMENT '로그인 아이디. 190 은 utf8mb4 에서 인덱스 키 길이 한계(3072B) 안에 드는 최대치',
  password_hash      VARCHAR(72)  NOT NULL COMMENT 'BCrypt 해시. 원문은 저장하지 않는다',
  role               ENUM('CUSTOMER','OWNER') NOT NULL,
  display_name       VARCHAR(32)  NOT NULL COMMENT '고객이면 닉네임, 사장이면 가게 이름. 화면 좌측 상단 표시용',
  email_verified     BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '인증 메일의 링크를 눌렀는지',
  token_version      INT          NOT NULL DEFAULT 0 COMMENT '비밀번호 변경 시 +1. 옛 JWT 를 무효화한다',
  created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_display_name (display_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 2. email_verification_tokens — 이메일 인증 토큰
-- ============================================================
-- 가입 시 한 건 발급하고 메일로 링크를 보낸다. 링크를 누르면
-- used_at 을 채우고 users.email_verified 를 TRUE 로 바꾼다.
--
-- 왜 별도 표인가 — users 에 컬럼으로 박으면 재발송할 때 옛 토큰이
-- 덮여서 사라진다. 여러 건을 남기면 "언제 몇 번 보냈는지"가 보이고,
-- 만료된 토큰으로 들어온 요청과 아예 없는 토큰을 구분해 안내할 수 있다.
--
-- token 은 서버가 만든 난수의 16진 문자열이다. 추측 가능한 값
-- (회원 번호, 이메일 해시)을 쓰면 남의 계정을 인증시킬 수 있다.

CREATE TABLE IF NOT EXISTS email_verification_tokens (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL,
  token       CHAR(64)    NOT NULL COMMENT '난수 32바이트의 16진 표기',
  expires_at  DATETIME(3) NOT NULL,
  used_at     DATETIME(3) NULL COMMENT 'NULL 이면 아직 안 쓴 토큰. 재사용을 막는다',
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  PRIMARY KEY (id),
  UNIQUE KEY uk_evt_token (token),
  KEY ix_evt_user (user_id, created_at),
  CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
