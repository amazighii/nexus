package com.buy01.orders.dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductEvent {
    private EventType eventType;
    private String productId;
    private String sellerId;
    private String productName;
    private BigDecimal price;
    private long quantity;
    private String description;
    private List<String> imageUrls = new ArrayList<>(); 
}
