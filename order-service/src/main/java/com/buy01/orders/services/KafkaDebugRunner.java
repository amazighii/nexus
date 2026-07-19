package com.buy01.orders.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

@Component
public class KafkaDebugRunner implements CommandLineRunner {

    @Autowired(required = false)
    private ConsumerFactory<?, ?> consumerFactory;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==============================================");
        if (consumerFactory == null) {
            System.out.println("❌ KAFKA ERROR: ConsumerFactory was never created by Spring!");
        } else {
            System.out.println("✅ KAFKA CONFIGS FOUND: " + consumerFactory.getConfigurationProperties());
        }
        System.out.println("==============================================");
    }
}
