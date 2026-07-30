-- ReviewTicket — 인증 대기 가입 요청
--
-- 정책 변경: 이메일 인증이 끝나기 전에는 회원을 만들지 않는다.
--
-- 이전 구조는 가입 버튼을 누르는 순간 users 행을 만들고 email_verified = false
-- 로 두었다. 문제는 인증하지 않은 사람이 남의 이메일과 닉네임을 선점해
-- 영구히 막아버릴 수 있다는 점이다. 여기에 임시로 담아두고, 링크를 눌러
-- 인증이 확인된 뒤에 users 로 옮긴다.
--
-- email 과 display_name 에 UNIQUE 를 거는 이유 — 두 사람이 같은 닉네임으로
-- 동시에 대기하는 것도 막아야 한다. 대기 중인 이름도 예약된 것으로 본다.
--
-- 실행:
--   Get-Content "C:\dev\ReviewTicket\BackEnd\DB\05_pending_signup.sql" | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u reviewticket -p

USE reviewticket;

CREATE TABLE IF NOT EXISTS pending_signups (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  email         VARCHAR(190) NOT NULL,
  password_hash VARCHAR(72)  NOT NULL COMMENT '가입 시점에 이미 해싱한다. 원문을 대기 표에 두지 않는다',
  role          ENUM('CUSTOMER','OWNER') NOT NULL,
  display_name  VARCHAR(32)  NOT NULL,
  token         CHAR(64)     NOT NULL COMMENT '난수 32바이트의 16진 표기. 인증 링크에 실린다',
  expires_at    DATETIME(3)  NOT NULL,
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '재발송 간격 제한에 쓴다',

  PRIMARY KEY (id),
  UNIQUE KEY uk_pending_token (token),
  UNIQUE KEY uk_pending_email (email),
  UNIQUE KEY uk_pending_display_name (display_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- email_verification_tokens 는 이제 쓰지 않는다.
-- 지우지 않고 두는 이유 — 비밀번호 재설정 기능에 같은 구조가 필요하다.
-- 그때 재활용한다.
