package com.buy01.orders.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.buy01.orders.models.ProductRef;

public interface ProductRefRepository extends MongoRepository<ProductRef, String> {
    Optional<ProductRef> findByProductId(String productId);
}
