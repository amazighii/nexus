package com.buy01.product.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotBlank
    private String productId;

    @Min(1)
    private long quantity;
}
