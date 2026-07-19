package com.buy01.orders.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.buy01.orders.dtos.ProductEvent;
import com.buy01.orders.models.ProductRef;
import com.buy01.orders.repositories.ProductRefRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ProductRefRepository productRefRepository;

    @KafkaListener(topics = "product-events", groupId = "order-service-group")
    public void handleProductEvent(ProductEvent event) {
        System.out.println("Received product event: " + event);

        switch (event.getEventType()) {
            case PRODUCT_CREATED ->
                handleProductCreated(event);
            case PRODUCT_UPDATED ->
                handleProductUpdated(event);
            case PRODUCT_DELETED ->
                handleProductDeleted(event);
        }

    }

    public void handleProductCreated(ProductEvent event) {
        System.out.println("Received product created event: " + event);

        ProductRef productRef = new ProductRef();

        productRef.setProductId(event.getProductId());
        productRef.setSellerId(event.getSellerId());
        productRef.setProductName(event.getProductName());
        productRef.setPrice(event.getPrice());
        productRef.setQuantity(event.getQuantity());
        productRef.setDescription(event.getDescription());
        if (event.getImageUrls() != null && !event.getImageUrls().isEmpty()) {
            productRef.setImageUrl(event.getImageUrls().get(0));
        }

        productRefRepository.save(productRef);
    }

    public void handleProductUpdated(ProductEvent event) {
        // Handle product updated event
    }

    public void handleProductDeleted(ProductEvent event) {
        // Handle product deleted event
    }
    
}
