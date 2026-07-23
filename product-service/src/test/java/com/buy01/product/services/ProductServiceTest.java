package com.buy01.product.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.buy01.product.dtos.ProductRequest;
import com.buy01.product.dtos.ProductResponse;
import com.buy01.product.kafka.KafkaProducerConfig;
import com.buy01.product.kafka.ProductEvent;
import com.buy01.product.models.EventType;
import com.buy01.product.models.Product;
import com.buy01.product.repositories.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @InjectMocks
    ProductService productService;

    @Test
    void getAllProducts_ShouldReturnProductResponses() {
        // Arrange
        Product product = createProduct("product-1", "seller-1");
        when(productRepository.findAll()).thenReturn(List.of(product));

        // Act
        List<ProductResponse> response = productService.getAllProducts();

        // Assert
        assertEquals(1, response.size());
        assertProductResponse(response.get(0), product);
    }

    @Test
    void getProductById_ShouldReturnProductResponse_WhenProductExists() {
        // Arrange
        Product product = createProduct("product-1", "seller-1");
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

        // Act
        ProductResponse response = productService.getProductById("product-1");

        // Assert
        assertProductResponse(response, product);
    }

    @Test
    void getProductById_ShouldThrowException_WhenProductDoesNotExist() {
        // Arrange
        when(productRepository.findById("missing-product")).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RuntimeException.class, () -> {
            productService.getProductById("missing-product");
        });
    }

    @Test
    void createProduct_ShouldSaveProductAndPublishCreatedEvent() {
        // Arrange
        ProductRequest request = createProductRequest();
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId("product-1");
            return product;
        });

        // Act
        ProductResponse response = productService.createProduct(request, "seller-1");

        // Assert
        assertNotNull(response);
        assertEquals("product-1", response.getId());
        assertEquals("seller-1", response.getSellerId());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(request.getPrice(), response.getPrice());
        assertEquals(request.getQuantity(), response.getQuantity());
        assertEquals(request.getImageUrls(), response.getImageUrls());

        ArgumentCaptor<ProductEvent> eventCaptor = ArgumentCaptor.forClass(ProductEvent.class);
        verify(kafkaTemplate).send(eq(KafkaProducerConfig.PRODUCT_TOPIC), eventCaptor.capture());

        ProductEvent event = eventCaptor.getValue();
        assertEquals(EventType.PRODUCT_CREATED, event.getEventType());
        assertEquals("product-1", event.getProductId());
        assertEquals("seller-1", event.getSellerId());
        assertEquals(request.getName(), event.getProductName());
        assertEquals(request.getImageUrls(), event.getImageUrls());
    }

    @Test
    void updateProduct_ShouldUpdateProduct_WhenProductBelongsToSeller() {
        // Arrange
        Product product = createProduct("product-1", "seller-1");
        ProductRequest request = createProductRequest();
        request.setName("Updated Product");

        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductResponse response = productService.updateProduct("product-1", request, "seller-1");

        // Assert
        assertEquals("Updated Product", response.getName());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(request.getPrice(), response.getPrice());
        assertEquals(request.getQuantity(), response.getQuantity());
        assertEquals(request.getImageUrls(), response.getImageUrls());
        verify(kafkaTemplate, never()).send(any(), any(ProductEvent.class));
    }

    @Test
    void updateProduct_ShouldThrowException_WhenProductDoesNotBelongToSeller() {
        // Arrange
        when(productRepository.findById("product-1")).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RuntimeException.class, () -> {
            productService.updateProduct("product-1", createProductRequest(), "seller-2");
        });

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_ShouldDeleteProductAndPublishDeletedEvent() {
        // Arrange
        Product product = createProduct("product-1", "seller-1");
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

        // Act
        productService.deleteProduct("product-1", "seller-1");

        // Assert
        verify(productRepository).delete(product);

        ArgumentCaptor<ProductEvent> eventCaptor = ArgumentCaptor.forClass(ProductEvent.class);
        verify(kafkaTemplate).send(eq(KafkaProducerConfig.PRODUCT_TOPIC), eventCaptor.capture());

        ProductEvent event = eventCaptor.getValue();
        assertEquals(EventType.PRODUCT_DELETED, event.getEventType());
        assertEquals("product-1", event.getProductId());
        assertEquals("seller-1", event.getSellerId());
        assertEquals(product.getName(), event.getProductName());
        assertEquals(product.getImageUrls(), event.getImageUrls());
    }

    @Test
    void deleteProduct_ShouldThrowException_WhenProductDoesNotBelongToSeller() {
        // Arrange
        when(productRepository.findById("product-1")).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RuntimeException.class, () -> {
            productService.deleteProduct("product-1", "seller-2");
        });

        verify(productRepository, never()).delete(any(Product.class));
        verify(kafkaTemplate, never()).send(any(), any(ProductEvent.class));
    }

    private ProductRequest createProductRequest() {
        ProductRequest request = new ProductRequest();
        request.setName("Gaming Keyboard");
        request.setDescription("Mechanical keyboard");
        request.setPrice(new BigDecimal("79.99"));
        request.setQuantity(12L);
        request.setImageUrls(List.of("https://cdn.example.com/product.png"));
        return request;
    }

    private Product createProduct(String id, String sellerId) {
        Product product = new Product();
        product.setId(id);
        product.setSellerId(sellerId);
        product.setName("Gaming Mouse");
        product.setDescription("Wireless mouse");
        product.setPrice(new BigDecimal("49.99"));
        product.setQuantity(8);
        product.setImageUrls(List.of("https://cdn.example.com/mouse.png"));
        product.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        product.setUpdatedAt(Instant.parse("2026-01-02T10:00:00Z"));
        return product;
    }

    private void assertProductResponse(ProductResponse response, Product product) {
        assertNotNull(response);
        assertEquals(product.getId(), response.getId());
        assertEquals(product.getName(), response.getName());
        assertEquals(product.getDescription(), response.getDescription());
        assertEquals(product.getPrice(), response.getPrice());
        assertEquals(product.getQuantity(), response.getQuantity());
        assertEquals(product.getSellerId(), response.getSellerId());
        assertEquals(product.getImageUrls(), response.getImageUrls());
        assertEquals(product.getCreatedAt(), response.getCreatedAt());
        assertEquals(product.getUpdatedAt(), response.getUpdatedAt());
    }
}
