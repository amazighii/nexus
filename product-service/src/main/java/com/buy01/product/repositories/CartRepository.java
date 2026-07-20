package com.buy01.product.repositories;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.buy01.product.models.Cart;

public interface CartRepository extends MongoRepository<Cart, ObjectId> {
    Optional<Cart> findByClientId(ObjectId clientId);
}
