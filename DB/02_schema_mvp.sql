-- ReviewTicket — 1단계 스키마 (AI 검증 + 업로드 테스트용)
--
-- 범위: 로그인/권한/주문 없음. 리뷰 페이지 하나가 도는 데 필요한 최소 집합.
-- 노션 「주요 테이블 (초안)」 8개 중 지금 필요한 3개만. 나머지(users/stores/
-- store_menus/orders/ticket_ledger)는 로그인·주문 붙일 때 추가한다.
--
-- 실행:
--   Get-Content "C:\dev\ReviewTicket\BackEnd\DB\02_schema_mvp.sql" | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u reviewticket -p

USE reviewticket;


-- ============================================================
-- 1. foods — 고정 카탈로그 5건
-- ============================================================
-- name 은 AI 모델이 반환하는 라벨 문자열과 정확히 일치해야 한다.
-- 오타 하나면 모든 리뷰가 '메뉴 불일치'로 거부되므로 UNIQUE 를 건다.
-- non_food 는 여기 없다 — 그건 클래스일 뿐 주문 가능한 메뉴가 아니다.

CREATE TABLE IF NOT EXISTS foods (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(32)  NOT NULL COMMENT 'AI 라벨과 동일: pizza/hamburger/chicken_wings/bibimbap/ramen',
  name_ko     VARCHAR(32)  NOT NULL COMMENT '화면 표시용',
  price       INT          NOT NULL COMMENT '고정값. 가게마다 다르지 않다',
  image_url   VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_foods_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO foods (name, name_ko, price) VALUES
  ('pizza',         '피자',   30000),
  ('hamburger',     '햄버거',  8500),
  ('chicken_wings', '치킨윙', 12000),
  ('bibimbap',      '비빔밥', 10000),
  ('ramen',         '라멘',   11000)
ON DUPLICATE KEY UPDATE name_ko = VALUES(name_ko), price = VALUES(price);


-- ============================================================
-- 2. reviews — 승인된 리뷰만 들어온다
-- ============================================================
-- 거부된 시도는 여기 안 남는다 (ai_rejections 로 간다).
-- 저장하는 것 셋: 후기 텍스트 / 압축된 이미지 / 평점.
-- 여기에 AI 근거를 같이 박아두는 이유 — 나중에 "이 사진이 왜 통과했나"를
-- 되짚을 수 있어야 실제 배달 사진 검증이 가능하다.

CREATE TABLE IF NOT EXISTS reviews (
  id                BIGINT       NOT NULL AUTO_INCREMENT,

  -- 주문 대신 개발용 메뉴 선택기가 고른 값. 로그인·주문 붙으면 orders 에서 온다.
  expected_food_id  BIGINT       NOT NULL,

  rating            TINYINT      NOT NULL COMMENT '1~5 정수. 0점·소수점 없음',
  content           VARCHAR(1000) NOT NULL COMMENT '최소 10자는 앱에서 검사',

  -- 이미지: 바이트는 DB에 넣지 않는다. 경로만.
  image_path        VARCHAR(255) NOT NULL COMMENT '1600px q85 축소본. 원본은 저장 안 함',
  image_sha256      CHAR(64)     NOT NULL COMMENT '완전 동일 파일 검출. UNIQUE 는 일부러 안 걸었다 — 지금은 AI 판정 테스트 단계라 같은 사진을 반복 업로드해야 한다. 중복 검출(BE-3.9) 붙일 때 ALTER TABLE 로 켠다',
  image_phash       BIGINT       NOT NULL COMMENT '유사 사진. 해밍거리 <=5 면 중복. 부호있는 64비트로 통일(Java long)',

  -- AI 판정 근거
  ai_predicted_label VARCHAR(32) NOT NULL COMMENT '음식 5개 중 argmax. 승인 건은 항상 expected 와 같다',
  ai_p_non_food     DECIMAL(7,6) NOT NULL COMMENT 'P(non_food). 판정에 실제로 쓰는 값',
  ai_probs          JSON         NOT NULL COMMENT '확률 6개 전부. tau 재조정·성능 재측정용',

  created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  PRIMARY KEY (id),
  KEY ix_reviews_sha256 (image_sha256),
  KEY ix_reviews_created (created_at),
  CONSTRAINT fk_reviews_food FOREIGN KEY (expected_food_id) REFERENCES foods (id),
  CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- 3. ai_rejections — 거부된 시도 로그
-- ============================================================
-- 이미지 파일은 저장하지 않는다 (거부는 메모리에서 버린다). 해시만 남긴다.
-- reason 을 따로 두는 이유: '음식 아님'과 '메뉴 불일치'는 원인이 완전히 달라서
-- 섞어 세면 모델을 고쳐야 할지 UI를 고쳐야 할지 알 수 없다.

CREATE TABLE IF NOT EXISTS ai_rejections (
  id                 BIGINT      NOT NULL AUTO_INCREMENT,
  expected_food_id   BIGINT      NOT NULL,
  reason             ENUM('not_food','menu_mismatch','duplicate') NOT NULL,

  ai_predicted_label VARCHAR(32) NULL COMMENT 'duplicate 로 걸리면 AI를 부르지 않아 NULL',
  ai_p_non_food      DECIMAL(7,6) NULL,
  ai_probs           JSON        NULL,

  image_sha256       CHAR(64)    NOT NULL,
  image_phash        BIGINT      NOT NULL,

  created_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  PRIMARY KEY (id),
  KEY ix_rejections_reason (reason, created_at),
  CONSTRAINT fk_rejections_food FOREIGN KEY (expected_food_id) REFERENCES foods (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
