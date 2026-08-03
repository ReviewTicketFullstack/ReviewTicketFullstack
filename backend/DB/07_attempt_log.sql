-- ReviewTicket — 로그인·가입·비밀번호변경·닉네임변경 시도 IP 로그
--
-- 성공/실패 상관없이 남긴다. 시도한 작업 자체가 롤백되는 경우에도
-- (서비스 계층은 REQUIRES_NEW 로 별도 트랜잭션에 저장) 이 표의 기록은 남는다.
--
-- 실행:
--   Get-Content "C:\dev\ReviewTicket\BackEnd\DB\07_attempt_log.sql" |
--     & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u reviewticket -p

USE reviewticket;

CREATE TABLE IF NOT EXISTS auth_attempt_logs (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  action        ENUM('LOGIN','SIGNUP','PASSWORD_CHANGE','NICKNAME_CHANGE') NOT NULL,
  email         VARCHAR(190) NULL COMMENT '시도 시점에 알 수 있는 경우만 채움',
  display_name  VARCHAR(32)  NULL COMMENT '가입/닉네임변경은 시도한 값, 그 외는 알 수 있으면 현재 값',
  ip            VARCHAR(45)  NOT NULL COMMENT 'IPv4/IPv6 모두 담는 길이',
  success       BOOLEAN      NOT NULL,
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  PRIMARY KEY (id),
  KEY ix_aal_ip_created (ip, created_at),
  KEY ix_aal_email_created (email, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
