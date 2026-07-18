package com.buy01.orders.dtos;

import lombok.Data;

@Data
public class CreateOrderDto {

    String firstname;

    String lastname;

    String phoneNumber;

    String address;

    Long quantity;

    String paymentMethod;

    String productId;
}
