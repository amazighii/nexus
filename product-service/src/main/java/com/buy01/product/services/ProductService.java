package com.buy01.product.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.Decimal128;
import org.springframework.data.domain.Sort;
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
    private final MongoTemplate mongoTemplate;

    public ProductService(
            ProductRepository productRepository,
            KafkaTemplate<String, ProductEvent> kafkaTemplate,
            MongoTemplate mongoTemplate) {
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.mongoTemplate = mongoTemplate;
    }

    // ── Public ──────────────────────────────────────────────
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProducts(BigDecimal minPrice, BigDecimal maxPrice, String sortOption) {
        if (minPrice != null && minPrice.signum() < 0) {
            throw new IllegalArgumentException("Minimum price cannot be negative.");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new IllegalArgumentException("Maximum price cannot be negative.");
        }
        if (minPrice != null && maxPrice != null) {
            if (minPrice.compareTo(maxPrice) > 0) {
                throw new IllegalArgumentException("Minimum price cannot be greater than maximum price.");
            }
        }

        Query query = new Query();
        if (minPrice != null || maxPrice != null) {
            Criteria price = Criteria.where("price");
            if (minPrice != null) {
                price = price.gte(new Decimal128(minPrice));
            }
            if (maxPrice != null) {
                price = price.lte(new Decimal128(maxPrice));
            }
            query.addCriteria(price);
        }
        query.with(toSort(sortOption));

        List<Product> products = mongoTemplate.find(query, Product.class);
        return products.stream().map(this::toResponse).collect(Collectors.toList());
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
        product.setCreatedAt(Instant.now());
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
        product.setUpdatedAt(Instant.now());
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

    private Sort toSort(String sortOption) {
        String normalized = sortOption == null ? "newest" : sortOption.trim().toLowerCase();
        return switch (normalized) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "name_desc" -> Sort.by(Sort.Direction.DESC, "name");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> throw new IllegalArgumentException("Unsupported product sort option: " + sortOption);
        };
    }
}
