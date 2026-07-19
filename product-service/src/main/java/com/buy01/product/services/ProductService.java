package com.buy01.product.services;

import java.util.List;
import java.util.stream.Collectors;

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

// import lombok.RequiredArgsConstructor;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

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
