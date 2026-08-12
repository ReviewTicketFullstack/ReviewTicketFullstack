-- 메뉴 표본 사진을 1장 -> 최대 5장으로 확장한다.
--
-- menu_table 은 이미 customer_order_table, customer_review_table 이 menu_id 로
-- 참조하고 있어 11_store_order_review_redesign.sql 처럼 DROP/CREATE 하면 안
-- 된다. ALTER TABLE 로만 덧붙인다.
--
-- 기존 menu_image_url 컬럼은 그대로 남긴다 — 앞으로는 "대표 사진"(목록·손님
-- 화면에 보이는 한 장) 전용이고, AI 대조는 아래 5칸만 쓴다.
--
-- 실행:
--   Get-Content "C:\dev\ReviewTicketFullstack\backend\DB\12_menu_sample_images.sql" | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u reviewticket -p -P 21096 --default-character-set=utf8mb4 reviewticket

ALTER TABLE menu_table
  ADD COLUMN sample_image_url_1 VARCHAR(255) NULL AFTER menu_image_url,
  ADD COLUMN sample_image_url_2 VARCHAR(255) NULL AFTER sample_image_url_1,
  ADD COLUMN sample_image_url_3 VARCHAR(255) NULL AFTER sample_image_url_2,
  ADD COLUMN sample_image_url_4 VARCHAR(255) NULL AFTER sample_image_url_3,
  ADD COLUMN sample_image_url_5 VARCHAR(255) NULL AFTER sample_image_url_4;
