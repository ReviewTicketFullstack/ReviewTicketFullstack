package com.reviewticket.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
// 실서비스 DB 를 비우지 않도록 reviewticket_test 로 붙는다
@ActiveProfiles({ "local", "test" })
class ServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
