package com.buy01.media.services;

import java.util.ArrayList;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.buy01.media.dto.ProductEvent;
import com.buy01.media.exception.MediaNotFound;
import com.buy01.media.models.Media;
import com.buy01.media.repositories.MediaRepository;

import io.minio.errors.MinioException;

@Service
public class MediaEventConsumer {

    private final MinioService minioService;
    private final MediaRepository mediaRepository;

    public MediaEventConsumer(
            MinioService minioService,
            MediaRepository mediaRepository) {
        this.minioService = minioService;
        this.mediaRepository = mediaRepository;
    }

    @KafkaListener(topics = "product-events", groupId = "media-service-group")
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
        if (event.getImageUrls() == null) {
            System.out.println("Received product event with no image URLs for ID: {}" + event.getProductId());
        }

        for (String url : event.getImageUrls()) {
            Media media = mediaRepository.findByUrl(url)
                    .orElseThrow(() -> new MediaNotFound());
            System.out.println("Associating media with URL: {} to product ID: {}" + url + event.getProductId());
            media.setProductId(event.getProductId());
            mediaRepository.save(media);
        }
    }

    public void handleProductUpdated(ProductEvent event) {

    }

    public void handleProductDeleted(ProductEvent event) {
        ArrayList<Media> media = mediaRepository.findAllByProductId(event.getProductId())
                .orElseThrow(() -> new MediaNotFound());

        for (Media md : media) {
            String url = md.getUrl();

            try {
                String objectName = url.substring(url.lastIndexOf("/") + 1);

                minioService.deleteFile(objectName);

                mediaRepository.deleteByUrl(url);
            } catch (MinioException e) {
                System.err.println("Failed to delete image: " + url + " → " + e.getMessage());
            }

        }

    }
}
