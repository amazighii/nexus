package com.buy01.orders.models;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.mongodb.lang.NonNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "product_ref")
public class ProductRef {
    @Id
    @Field("product_id")
    String productId;

    @NonNull
    String sellerId;

    @NonNull
    String productName;

    @NonNull
    @Field(targetType = FieldType.DECIMAL128) // Forces MongoDB to store BigDecimal as a number!
    BigDecimal price;

    @NonNull
    long quantity;

    @NonNull
    String description;

    @Field("image_url")
    String imageUrl;
}
