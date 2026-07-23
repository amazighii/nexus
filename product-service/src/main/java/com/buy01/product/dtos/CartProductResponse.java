package com.buy01.product.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartProductResponse {
    private String productId;
    private String productName;
    private BigDecimal price;
    private long quantity;
    private String imageUrl;
}
