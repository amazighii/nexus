package com.buy01.product.controllers;

import com.buy01.product.dtos.ProductRequest;
import com.buy01.product.dtos.ProductResponse;
import com.buy01.product.dtos.CartItemRequest;
import com.buy01.product.dtos.CartResponse;
import com.buy01.product.dtos.TopProductDto;
import com.buy01.product.services.CartService;
import com.buy01.product.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CartService cartService;
    private final ProductAnalyticsClient analyticsClient;

    @GetMapping("/cart")
    public ResponseEntity<CartResponse> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCart(auth.getName()));
    }

    @PostMapping("/cart")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody CartItemRequest request, Authentication auth) {
        return ResponseEntity.ok(cartService.addItem(auth.getName(), request));
    }

    @DeleteMapping("/cart")
    public ResponseEntity<Void> removeFromCart(@RequestParam(required = false) String productId, Authentication auth) {
        cartService.remove(auth.getName(), productId);
        return ResponseEntity.noContent().build();
    }

    // ── Public ──────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(value = "min-price", required = false) BigDecimal minPrice,
            @RequestParam(value = "max-price", required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice));
    }

    @GetMapping("/name")
    public ResponseEntity<List<ProductResponse>> searchProductsByName(@RequestParam("q") String searchTerm) {
        return ResponseEntity.ok(productService.searchProductsByName(searchTerm));
    }

    @GetMapping("/client/most-buying-products")
    public ResponseEntity<List<TopProductDto>> clientMostBuyingProducts(
            Authentication auth,
            @RequestParam(value = "limit", defaultValue = "5") Long limit) {
        return ResponseEntity.ok(analyticsClient.clientMostBuyingProducts(auth.getName(), "CLIENT", limit));
    }

    @GetMapping("/client/best-products")
    public ResponseEntity<List<TopProductDto>> clientBestProducts(
            Authentication auth,
            @RequestParam(value = "limit", defaultValue = "5") Long limit) {
        return ResponseEntity.ok(analyticsClient.clientBestProducts(auth.getName(), "CLIENT", limit));
    }

    @GetMapping("/seller/best-selling-products")
    public ResponseEntity<List<TopProductDto>> sellerBestSellingProducts(
            Authentication auth,
            @RequestParam(value = "limit", defaultValue = "5") Long limit) {
        return ResponseEntity.ok(analyticsClient.sellerBestSellingProducts(auth.getName(), "SELLER", limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // ── Seller only ─────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request,
            Authentication auth) {

        String sellerId = auth.getName(); // userId from JWT
        System.out.println("\nCreating product for sellerId: " + sellerId + "\n");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request, sellerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request,
            Authentication auth) {

        String sellerId = auth.getName();
        return ResponseEntity.ok(productService.updateProduct(id, request, sellerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable String id,
            Authentication auth) {

        String sellerId = auth.getName();
        productService.deleteProduct(id, sellerId);
        return ResponseEntity.noContent().build();
    }
}
