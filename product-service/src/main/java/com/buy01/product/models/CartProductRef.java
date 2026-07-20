package com.buy01.product.models;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A product snapshot kept in a client's cart for fast cart rendering. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartProductRef {
    private String productId;
    private String productName;
    private BigDecimal price;
    private long quantity;
    private String imageUrl;
}
