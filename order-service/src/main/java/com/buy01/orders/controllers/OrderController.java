package com.buy01.orders.controllers;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buy01.orders.dtos.ClientOrdersList;
import com.buy01.orders.dtos.CreateOrderDto;
import com.buy01.orders.dtos.CreateOrderMessage;
import com.buy01.orders.dtos.ReturnMessage;
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

    @GetMapping("/seller")
    ResponseEntity<ClientOrdersList> getSellerOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {

        return ResponseEntity.ok(orderService.getSellerOrders(userId,  userRole));
    }

    @PostMapping
    ResponseEntity<CreateOrderMessage> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody CreateOrderDto createOrderDto) {

        CreateOrderMessage responseMessage = orderService.createOrder(userId, createOrderDto);
        return ResponseEntity.ok(responseMessage);
    }

    @PutMapping("/remove/{id}")
    ResponseEntity<ReturnMessage> removeOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable("id") String orderId) {

        ReturnMessage responseMesasge = orderService.removeOrder(userId, orderId);
        return ResponseEntity.ok(responseMesasge);
    }

    @PutMapping("/cancel/{id}")
    ResponseEntity<ReturnMessage> cancelOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable("id") String orderId) {
        ReturnMessage responseMesasge = orderService.cancelOrder(userId, orderId);
        return ResponseEntity.ok(responseMesasge);
    }

    @GetMapping("/search")
    ResponseEntity<ClientOrdersList> searchOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {

        ClientOrdersList response = orderService.searchOrders(userId, status, date);
        return ResponseEntity.ok(response);
    }
}
