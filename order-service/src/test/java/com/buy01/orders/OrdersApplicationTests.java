package com.buy01.orders;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import com.buy01.orders.repositories.OrderRepository;

@SpringBootTest
class OrdersApplicationTests {
	@Mock
	OrderRepository productRepository;

	@Test
	void contextLoads() {
	}

}
