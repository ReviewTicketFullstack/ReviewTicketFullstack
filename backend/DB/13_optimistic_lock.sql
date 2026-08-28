-- users 에 낙관적 락 컬럼을 추가한다.
--
-- tickets 는 읽고-검사하고-쓰는 사이가 비어 있었다. 같은 계정으로 이벤트 주문이
-- 동시에 들어오면 두 요청이 같은 값을 읽고 각자 검사를 통과해, 보유 티켓보다
-- 많은 주문이 생성된다(증식). 반대로 리뷰 저장의 티켓 반환이 겹치면 나중 쪽이
-- 앞선 쪽을 덮어써 한 장이 사라진다(유실). 둘 다 에러도 로그도 안 남는다.
--
-- 다만 이 컬럼은 주 수단이 아니라 그물이다. 실제로 티켓을 지키는 것은 조회
-- 시점에 행을 잠그는 UserRepository.findByIdForUpdate(비관적 락)다.
--
-- 낙관적 락만으로는 부족했던 이유 — 주문 생성은 customer_order_table 에 INSERT
-- 하면서 외래키 때문에 users 행에 공유 락을 먼저 건다. 그 뒤 티켓 UPDATE 가
-- 배타 락으로 올라가려 하면 두 요청이 서로를 기다려 데드락(MySQL 1213)이 났다.
-- 처음부터 배타 락을 쥐고 들어가면 승격이 없어 데드락도 없다.
--
-- 이 컬럼이 남아 있는 이유 — 잠금 없이 tickets 를 고치는 경로가 나중에 생겼을 때
-- 값이 조용히 덮어써지는 대신 커밋이 실패해 드러나게 하는 안전장치다.
--
-- 기존 행은 DEFAULT 0 으로 채워지므로 별도 백필이 필요 없다.
-- ddl-auto:validate 라 이 스크립트를 먼저 실행해야 서버가 뜬다.
--
-- 실행:
--   Get-Content "C:\dev\ReviewTicketFullstack\backend\DB\13_optimistic_lock.sql" | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u reviewticket -p -P 21096 --default-character-set=utf8mb4 reviewticket

ALTER TABLE users
  ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락. Hibernate 가 갱신할 때마다 +1 한다. 애플리케이션이 직접 읽거나 쓰지 않는다';
