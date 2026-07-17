package com.buy01.product.repositories;

import com.buy01.product.models.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findBySellerId(String sellerId);

    Optional<Product> findByIdAndSellerId(String id, String sellerId);
}