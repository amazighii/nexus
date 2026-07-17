package com.buy01.product.kafka;

import java.util.ArrayList;
import java.util.List;

import com.buy01.product.models.EventType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductEvent {
    // "PRODUCT_CREATED"
    // "PRODUCT_UPDATED"
    // "PRODUCT_DELETED"
    private EventType eventType;
    private String productId;
    private String sellerId;
    private String productName;
    private List<String> imageUrls = new ArrayList<>(); 
}