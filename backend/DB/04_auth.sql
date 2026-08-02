-- ReviewTicket 회원·인증 스키마
--
-- 실행:
--   Get-Content "C:\dev\ReviewTicket\BackEnd\DB\04_auth.sql" |
--     & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u reviewticket -p

USE reviewticket;

CREATE TABLE IF NOT EXISTS users (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  email              VARCHAR(190) NOT NULL,
  password_hash      VARCHAR(72)  NOT NULL,
  role               ENUM('CUSTOMER','OWNER') NOT NULL,
  display_name       VARCHAR(32)  NOT NULL,
  email_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
  token_version      INT          NOT NULL DEFAULT 0,
  created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                      ON UPDATE CURRENT_TIMESTAMP(3),

  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_display_name (display_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
