package com.buy01.orders.dtos;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CreateOrderDto {

    String firstname;

    String lastname;

    String phoneNumber;

    String address;

    String paymentMethod;

    /** Product IDs and quantities submitted by direct purchase or cart checkout. */
    List<ProductOrder> productIds = new ArrayList<>();

    /** Preferred name for new clients; productIds remains supported for compatibility. */
    List<ProductOrder> products = new ArrayList<>();

    public List<ProductOrder> requestedProducts() {
        return products != null && !products.isEmpty() ? products : productIds;
    }
}
