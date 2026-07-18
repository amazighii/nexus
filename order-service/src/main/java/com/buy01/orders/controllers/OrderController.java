package com.buy01.orders.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buy01.orders.dtos.ClientOrdersList;
import com.buy01.orders.dtos.CreateOrderDto;
import com.buy01.orders.dtos.CreateOrderMessage;
import com.buy01.orders.services.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/client")

    ResponseEntity<ClientOrdersList> getClientOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {

        return ResponseEntity.ok(orderService.getClientOrders(userId));
    }

    @PostMapping
    ResponseEntity<CreateOrderMessage> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody CreateOrderDto createOrderDto) {

        CreateOrderMessage responseMessage = orderService.createOrder(userId, createOrderDto);
        return ResponseEntity.ok(responseMessage);
    }
}
