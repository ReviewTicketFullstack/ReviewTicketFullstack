-- ReviewTicket — 비밀번호 재설정 토큰
--
-- 비밀번호를 잊은 사용자가 이메일 인증으로 새 비밀번호를 정한다.
-- 기존 비밀번호는 묻지 않는다 — 모르니까 재설정하는 것이다.
--
-- 흐름:
--   요청(이메일) → 토큰 발급 + 메일 → 링크의 인증 버튼 → 새 비밀번호 입력 → 변경
--
-- 실행:
--   Get-Content "C:\dev\ReviewTicket\BackEnd\DB\06_password_reset.sql" |
--     & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u reviewticket -p

USE reviewticket;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL,
  token       CHAR(64)    NOT NULL COMMENT '난수 32바이트의 16진 표기. 재설정 링크에 실린다',
  expires_at  DATETIME(3) NOT NULL,
  used_at     DATETIME(3) NULL COMMENT 'NULL 이면 아직 안 쓴 토큰. 한 번 쓰면 재사용 불가',
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '재발송 간격 제한에 쓴다',

  PRIMARY KEY (id),
  UNIQUE KEY uk_prt_token (token),
  KEY ix_prt_user (user_id, created_at),
  CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
