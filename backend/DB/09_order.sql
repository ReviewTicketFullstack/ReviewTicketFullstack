-- ReviewTicket — 주문
--
-- 주문 1건 = 메뉴 1개다. 장바구니와 수량 개념이 없고, 메뉴 하나를 고르면
-- 바로 주문이 만들어진다.
--
-- menu_name, price, review_event 는 menus 를 가리키는 대신 주문 시점의 값을
-- 복사해 둔다(스냅샷). menu_id 로 매번 조인하면 사장이 가격을 올리는 순간
-- 이미 지나간 주문의 금액까지 따라 바뀌고, 메뉴가 지워지면 과거 주문의
-- 이름이 사라진다. 영수증은 그때 그 값으로 남아야 한다.
--
-- 실행:
--   Get-Content "C:\dev\ReviewTicketFullstack\backend\DB\09_order.sql" |
--     & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h 127.0.0.1 -P 21096 -u root -p

USE reviewticket;

CREATE TABLE IF NOT EXISTS orders (
  id              BIGINT      NOT NULL AUTO_INCREMENT,
  user_id         BIGINT      NOT NULL COMMENT '주문한 고객',
  store_id        BIGINT      NOT NULL,
  menu_id         BIGINT      NOT NULL,
  menu_name       VARCHAR(50) NOT NULL COMMENT '주문 시점 스냅샷',
  price           INT         NOT NULL COMMENT '주문 시점 스냅샷. 원 단위 정수',
  review_event    BOOLEAN     NOT NULL COMMENT '주문 시점 스냅샷. 리뷰 대상 주문인지',
  review_deadline DATETIME(3) NOT NULL COMMENT '리뷰 작성 마감 시각',
  created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '주문 시각',

  PRIMARY KEY (id),
  -- 내 주문 내역을 최신순으로 읽는 것이 유일한 조회 패턴이라 이 조합으로 묶는다.
  KEY ix_orders_user_created (user_id, created_at DESC),
  KEY ix_orders_store (store_id),
  CONSTRAINT fk_orders_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
  CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES stores (id),
  -- 메뉴는 지워질 수 있다. 지워져도 주문은 남아야 하므로 CASCADE 를 걸지 않는다
  -- (이름과 가격은 위 스냅샷 컬럼에 이미 복사돼 있다).
  CONSTRAINT fk_orders_menu  FOREIGN KEY (menu_id)  REFERENCES menus (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
