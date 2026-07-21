package com.buy01.product.controllers;

import java.util.List;

import com.buy01.product.dtos.TopProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ORDER-SERVICE")
public interface ProductAnalyticsClient {
    @GetMapping("/api/orders/client/most-buying-products")
    List<TopProductDto> clientMostBuyingProducts(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam("limit") Long limit);

    @GetMapping("/api/orders/client/best-products")
    List<TopProductDto> clientBestProducts(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam("limit") Long limit);

    @GetMapping("/api/orders/seller/best-selling-products")
    List<TopProductDto> sellerBestSellingProducts(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam("limit") Long limit);
}
