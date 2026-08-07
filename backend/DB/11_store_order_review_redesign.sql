-- ReviewTicket — 가게, 메뉴, 주문, 리뷰 재설계
--
-- reviewticket-store-order-review-api.html 설계서 2장 그대로 옮긴 것이다.
-- 기존 stores/menus/orders 를 대체한다. 예전 reviews 표(food-cls AI 판정용,
-- store/menu/user 와 연결이 없어 이번 기능에 못 쓴다)도 이 김에 정리한다.
--
-- 실행 전 반드시 백업할 것 — 기존 stores/menus/orders 에 실데이터가 있다.
-- 이 폴더의 backup/ 에 이미 한 번 떠 둔 백업이 있다(pre_redesign_*.sql).
-- 시간이 지났다면 새로 하나 더 뜨는 편이 안전하다.
--
-- 실행:
--   & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h 127.0.0.1 -P 21096 -u root -p ^
--     --default-character-set=utf8mb4 -e "source C:/dev/ReviewTicketFullstack/backend/DB/11_store_order_review_redesign.sql"

USE reviewticket;

DROP TABLE IF EXISTS customer_review_table;
DROP TABLE IF EXISTS customer_order_table;
DROP TABLE IF EXISTS menu_table;
DROP TABLE IF EXISTS store_table;
DROP TABLE IF EXISTS reviews;
-- 옛 orders 가 menus 를 FK 로 참조하므로(fk_orders_menu), orders 를 먼저
-- 지워야 menus 를 지울 수 있다. 자식(orders) -> 자식(menus) -> 부모(stores) 순서.
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS menus;
DROP TABLE IF EXISTS stores;

-- 이 셋은 이번 재설계와 무관하게 이미 죽어 있던 테이블이다. 정리 김에 같이 치운다.
--   email_verification_tokens: "비밀번호 재설정 때 재활용한다"던 옛 계획이 무산되고
--     password_reset_tokens 를 새로 만들면서 대신했다. 재활용될 일이 없다.
--   ai_rejections: ai_rejections 가 참조하므로 foods 보다 먼저 지운다.
--   foods: 옛 food-cls(6클래스 분류) 프로젝트용. 지금은 메뉴사진과 리뷰사진을
--     직접 비교하는 방식이라 음식 카테고리 테이블 자체가 필요 없다.
DROP TABLE IF EXISTS email_verification_tokens;
DROP TABLE IF EXISTS ai_rejections;
DROP TABLE IF EXISTS foods;

CREATE TABLE store_table (
  store_id      BIGINT       NOT NULL AUTO_INCREMENT,
  owner_id      BIGINT       NOT NULL COMMENT 'users.id. 가게 하나당 사장 하나라 UNIQUE',
  store_name    VARCHAR(32)  NOT NULL COMMENT 'users.display_name 과 항상 같은 값으로 맞춘다 (4.4/AccountService 양쪽에서 동기화)',
  logo_url      VARCHAR(255) NULL COMMENT '없으면 NULL, 화면은 회색 자리표시',
  review_number INT          NOT NULL DEFAULT 0 COMMENT '총 리뷰 개수. 리뷰가 저장될 때 함께 갱신',
  -- DECIMAL 이 아니라 DOUBLE 을 쓴다. Java 필드가 double 이라 Hibernate
  -- ddl-auto:validate 가 DECIMAL 을 다른 타입으로 보고 기동을 거부한다.
  review_value  DOUBLE       NOT NULL DEFAULT 0.0 COMMENT '평균 별점. 매번 세지 않고 저장해 둔다',
  is_reviewing  BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '리뷰이벤트 대상 메뉴가 하나라도 있는지. 가게 생성 시 한 번 계산',
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- ON UPDATE 를 일부러 안 건다. DB의 ON UPDATE CURRENT_TIMESTAMP 는 어느 컬럼이
  -- 바뀌었는지 가리지 않고 행 전체에 걸리므로, 리뷰 통계 갱신(review_number,
  -- review_value)에도 같이 움직여 버린다. 이름/로고 변경에만 반응하게 하려면
  -- 애플리케이션(Store.changeInfo)이 이 컬럼을 직접 써야 한다.
  latest_update DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                  COMMENT '이름/로고 변경 때만 갱신. Store.changeInfo() 가 직접 쓴다',

  PRIMARY KEY (store_id),
  UNIQUE KEY uk_store_owner (owner_id),
  UNIQUE KEY uk_store_name (store_name),
  CONSTRAINT fk_store_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE menu_table (
  menu_id            BIGINT       NOT NULL AUTO_INCREMENT,
  store_id           BIGINT       NOT NULL,
  menu_name          VARCHAR(32)  NOT NULL COMMENT '프로토타입 고정 5종, 사장이 못 바꿈',
  menu_price         INT          NOT NULL COMMENT '원 단위 정수',
  menu_image_url     VARCHAR(255) NULL COMMENT 'AI 검증의 비교 기준 사진',
  review_event       BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '리뷰 이벤트 대상 메뉴인지',
  menu_created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  menu_latest_update DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

  PRIMARY KEY (menu_id),
  KEY ix_menu_store (store_id),
  CONSTRAINT fk_menu_store FOREIGN KEY (store_id) REFERENCES store_table (store_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer_order_table (
  order_id           BIGINT      NOT NULL AUTO_INCREMENT,
  customer_id        BIGINT      NOT NULL COMMENT '주문한 고객. users.id',
  store_id           BIGINT      NOT NULL,
  menu_id            BIGINT      NOT NULL,
  menu_name          VARCHAR(32) NOT NULL COMMENT '주문 시점 스냅샷',
  menu_price         INT         NOT NULL COMMENT '주문 시점 스냅샷',
  review_event_apply BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '고객이 리뷰이벤트에 실제로 참여 신청했는지. true 인 주문에만 티켓이 잠긴다',
  review_deadline    INT         NULL COMMENT '리뷰 작성 가능 기간(초). apply=true 일 때만 채움, 앱 전역 정책의 스냅샷',
  ordered_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  expire_time        DATETIME(3) NULL COMMENT 'ordered_at + review_deadline. apply=true 일 때만 채움',

  PRIMARY KEY (order_id),
  KEY ix_order_customer (customer_id, ordered_at DESC),
  KEY ix_order_store (store_id),
  CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_order_store    FOREIGN KEY (store_id)    REFERENCES store_table (store_id),
  CONSTRAINT fk_order_menu     FOREIGN KEY (menu_id)     REFERENCES menu_table (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer_review_table (
  review_id         BIGINT        NOT NULL AUTO_INCREMENT,
  order_id          BIGINT        NOT NULL COMMENT 'UNIQUE. 주문 하나에 리뷰 하나만',
  store_id          BIGINT        NOT NULL COMMENT '가게별 리뷰 조회 시 주문 표를 거치지 않으려고 함께 둠',
  menu_id           BIGINT        NOT NULL,
  user_id           BIGINT        NOT NULL COMMENT '리뷰를 쓴 사람',
  -- INT 로 둔다(TINYINT 로도 충분하지만). Java 필드가 int 라 Hibernate의
  -- ddl-auto:validate 가 스키마 검증할 때 타입을 가장 무난하게 맞춰 통과시키는
  -- 조합이다. 값 범위는 아래 CHECK 제약이 어차피 1~5로 막는다.
  review_rating     INT           NOT NULL COMMENT '1~5, CHECK 제약으로 범위를 막는다',
  review_content    VARCHAR(255)  NOT NULL COMMENT '10~50자, 검증은 애플리케이션 단',
  review_image_url  VARCHAR(255)  NOT NULL COMMENT 'AI 검증 통과 순간 디스크에 기록되므로, 값이 있다는 것 자체가 통과했다는 뜻',
  review_created_at DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 마찬가지로 DECIMAL 대신 DOUBLE(Java double 과 대응). 실제로 이 컬럼에서
  -- "found decimal, but expecting float(53)" 에러로 걸렸었다.
  image_similarity  DOUBLE        NOT NULL COMMENT 'AI 유사도. 0.800 이상만 저장됨',
  compare_image_url VARCHAR(255)  NOT NULL COMMENT '판정 당시 비교했던 메뉴 사진 스냅샷',

  PRIMARY KEY (review_id),
  UNIQUE KEY uk_review_order (order_id),
  KEY ix_review_store (store_id),
  KEY ix_review_user (user_id),
  CONSTRAINT ck_review_rating CHECK (review_rating BETWEEN 1 AND 5),
  CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES customer_order_table (order_id),
  CONSTRAINT fk_review_store FOREIGN KEY (store_id) REFERENCES store_table (store_id),
  CONSTRAINT fk_review_menu  FOREIGN KEY (menu_id)  REFERENCES menu_table (menu_id),
  CONSTRAINT fk_review_user  FOREIGN KEY (user_id)  REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
