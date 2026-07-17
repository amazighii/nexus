package com.buy01.product.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Setter
@Getter

@Document(collection = "products")
@NoArgsConstructor
public class Product {

    @Id
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private long quantity;
    // To check later with Media
    private List<String> imageUrls; // set later via Media Service 
    private String sellerId; // reference to User ID of the seller
    private Instant createdAt;
    private Instant updatedAt;
}
