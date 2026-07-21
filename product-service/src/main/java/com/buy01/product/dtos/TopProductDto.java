package com.buy01.product.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDto {
    private String productId;
    private String productName;
    private Long totalQuantity;
    private BigDecimal totalSpent;
    private String imageUrl;
}
