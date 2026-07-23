package com.buy01.product.models;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "carts")
public class Cart {
    @Id
    private ObjectId id;

    @Field("client_id")
    private ObjectId clientId;

    private List<CartProductRef> products = new ArrayList<>();
}
