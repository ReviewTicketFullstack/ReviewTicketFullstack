-- ReviewTicket — 가게와 메뉴
--
-- 가게 이름은 stores.name 이 정답이다. 사장 회원가입이 확정되는 순간
-- (이메일 인증을 마치고 users 행이 만들어질 때) stores 행도 함께 만들면서
-- users.display_name 을 복사한다. 그래서 사장은 가입만 하면 가게가 생긴다.
--
-- users.display_name 을 그대로 쓰지 않는 이유 — 그 값은 '계정 표시명'이고,
-- 가게 이름은 나중에 따로 바뀔 수 있어야 한다. 한 사장이 가게를 여러 개
-- 가지게 되는 경우도 users 쪽에는 담을 자리가 없다.
--
-- 실행:
--   Get-Content "C:\dev\ReviewTicketFullstack\backend\DB\08_store_menu.sql" |
--     & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h 127.0.0.1 -P 21096 -u root -p

USE reviewticket;

CREATE TABLE IF NOT EXISTS stores (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  owner_user_id BIGINT       NOT NULL,
  name          VARCHAR(32)  NOT NULL,
  image_url     VARCHAR(500) NULL COMMENT '목록 썸네일. NULL 이면 프론트가 회색 박스로 대체한다',
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                 ON UPDATE CURRENT_TIMESTAMP(3),

  PRIMARY KEY (id),
  -- 이름 UNIQUE 는 users.display_name 이 이미 UNIQUE 라 사실상 보장되지만,
  -- 가게 이름을 따로 바꿀 수 있게 되면 그때는 이 제약이 유일한 방어선이 된다.
  UNIQUE KEY uk_stores_name (name),
  KEY ix_stores_owner (owner_user_id),
  CONSTRAINT fk_stores_owner FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS menus (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  store_id     BIGINT       NOT NULL,
  name         VARCHAR(50)  NOT NULL,
  price        INT          NOT NULL COMMENT '원 단위 정수. 콤마와 "원" 없이 18000 처럼 담는다',
  image_url    VARCHAR(500) NULL,
  review_event BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '주문하면 리뷰를 쓸 수 있는 메뉴인지',
  created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                ON UPDATE CURRENT_TIMESTAMP(3),

  PRIMARY KEY (id),
  KEY ix_menus_store (store_id),
  CONSTRAINT fk_menus_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
