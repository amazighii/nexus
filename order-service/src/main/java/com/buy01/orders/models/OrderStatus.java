package com.buy01.orders.models;

public enum OrderStatus {
    PENDING("Order received, waiting for confirmation"),
    PROCESSING("Order is being packed"),
    SHIPPED("Order is on the way"),
    DELIVERED("Order delivered successfully"),
    CANCELLED("Order was cancelled");

    private final String description;

    private OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinalStatus() {
        return this == DELIVERED || this == CANCELLED;
    }
}
