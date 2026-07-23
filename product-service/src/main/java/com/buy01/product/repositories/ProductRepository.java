package com.buy01.product.repositories;

import com.buy01.product.models.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findBySellerId(String sellerId);

    Optional<Product> findByIdAndSellerId(String id, String sellerId);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    List<Product> findByPriceGreaterThanEqual(BigDecimal minPrice);

    List<Product> findByPriceLessThanEqual(BigDecimal maxPrice);

    List<Product> findByPriceGreaterThanEqualAndPriceLessThanEqual(
            BigDecimal minPrice,
            BigDecimal maxPrice);
}
