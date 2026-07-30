-- 테스트 전용 DB.
--
-- 왜 필요한가: 테스트가 reviews / ai_rejections 를 통째로 비운다 (E2E 테스트가
-- 자기가 만든 행을 정리해야 하므로). 실서비스 DB 에 붙여 두면 `gradlew test`
-- 한 번에 데모 데이터가 전부 사라진다. 실제로 한 번 날렸다.
--
-- 실행 (root 로):
--   Get-Content "C:\dev\ReviewTicket\BackEnd\DB\03_test_database.sql" | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p --default-character-set=utf8mb4
--
-- 그 다음 스키마를 테스트 DB 에 넣는다:
--   (Get-Content "C:\dev\ReviewTicket\BackEnd\DB\02_schema_mvp.sql") -replace 'USE reviewticket;','USE reviewticket_test;' | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p --default-character-set=utf8mb4

CREATE DATABASE IF NOT EXISTS reviewticket_test
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON reviewticket_test.* TO 'reviewticket'@'localhost';
FLUSH PRIVILEGES;
