package com.buy01.product.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartResponse {
    private String id;
    private String clientId;
    private List<CartProductResponse> products;
}
