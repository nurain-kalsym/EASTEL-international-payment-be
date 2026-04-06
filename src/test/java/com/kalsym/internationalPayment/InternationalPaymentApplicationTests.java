package com.kalsym.internationalPayment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // force H2
class InternationalPaymentApplicationTests {

	@Test
	void contextLoads() {
	}

}
