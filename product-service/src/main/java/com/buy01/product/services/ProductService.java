package com.buy01.product.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.buy01.product.dtos.ProductRequest;
import com.buy01.product.dtos.ProductResponse;
import com.buy01.product.exceptions.ProductAccessDeniedException;
import com.buy01.product.exceptions.ProductNotFoundException;
import com.buy01.product.kafka.KafkaProducerConfig;
import com.buy01.product.kafka.ProductEvent;
import com.buy01.product.models.EventType;
import com.buy01.product.models.Product;
import com.buy01.product.repositories.ProductRepository;



@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;
    @Autowired
    private MongoTemplate mongoTemplate;

    public ProductService(ProductRepository productRepository, KafkaTemplate<String, ProductEvent> kafkaTemplate) {
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // ── Public ──────────────────────────────────────────────
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        System.out.println("maxPrice: " + maxPrice + "  minPrice: " + minPrice);
        System.out.println("db name: " + mongoTemplate.getDb().getName());

        System.out.println("DB: " + mongoTemplate.getDb().getName());
        System.out.println("Collection count: " +
                mongoTemplate.getCollection("products").countDocuments());
        List<Product> products;
        if (minPrice != null && maxPrice != null) {
            if (minPrice.compareTo(maxPrice) > 0) {
                throw new IllegalArgumentException("Minimum price cannot be greater than maximum price.");
            }

            products = productRepository.findByPriceBetween(minPrice, maxPrice);
            products = findByPriceBetweenMinAndMax(minPrice, maxPrice);
        } else if (minPrice != null) {
            products = productRepository.findByPriceGreaterThanEqual(minPrice);
        } else if (maxPrice != null) {
            products = productRepository.findByPriceLessThanEqual(maxPrice);
        } else {
            products = productRepository.findAll();
        }

        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private List<Product> findByPriceBetweenMinAndMax(BigDecimal min, BigDecimal max) {
        Query query = new Query();

        query.addCriteria(
                Criteria.where("price")
                        .gte(new Decimal128(min))
                        .lte(new Decimal128(max)));

        List<Product> result = mongoTemplate.find(query, Product.class);

        return result;
    }

    public List<ProductResponse> searchProductsByName(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return getAllProducts();
        }
        return productRepository.findByNameContainingIgnoreCase(searchTerm.trim())
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found."));
        return toResponse(product);
    }

    // ── Seller only ─────────────────────────────────────────
    public ProductResponse createProduct(ProductRequest request, String sellerId) {
        Product product = new Product();
        product.setSellerId(sellerId);
        applyRequest(product, request);

        Product saved = productRepository.save(product);

        kafkaTemplate.send(KafkaProducerConfig.PRODUCT_TOPIC,
                new ProductEvent(
                        EventType.PRODUCT_CREATED,
                        saved.getId(),
                        sellerId,
                        saved.getName(),
                        saved.getPrice(),
                        saved.getQuantity(),
                        saved.getDescription(),
                        saved.getImageUrls()));

        return toResponse(saved);
    }

    public ProductResponse updateProduct(String id, ProductRequest request, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        if (!sellerId.equals(product.getSellerId())) {
            throw new ProductAccessDeniedException("You do not have permission to modify this product.");
        }

        applyRequest(product, request);
        Product saved = productRepository.save(product);

        return toResponse(saved);
    }

    public void deleteProduct(String id, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found."));

        if (!sellerId.equals(product.getSellerId())) {
            throw new ProductAccessDeniedException("You do not have permission to delete this product.");
        }

        productRepository.delete(product);

        kafkaTemplate.send(KafkaProducerConfig.PRODUCT_TOPIC,
                new ProductEvent(
                        EventType.PRODUCT_DELETED,
                        id,
                        sellerId,
                        product.getName(),
                        product.getPrice(),
                        product.getQuantity(),
                        product.getDescription(),
                        product.getImageUrls()));
    }

    // ── Helpers ─────────────────────────────────────────────
    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        if (request.getImageUrls() != null) {
            product.setImageUrls(request.getImageUrls());
        }
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setQuantity(product.getQuantity());
        response.setSellerId(product.getSellerId());
        response.setImageUrls(product.getImageUrls());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }
}
