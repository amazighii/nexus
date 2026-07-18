package com.buy01.orders.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientOrderDto {
    String Id;

    String firstname;

    String lastname;

    String phoneNumber;

    String address;

    String clientId;

    String status;

    Long quantity;

    Double price;

    String paymentMethod;

    String date;
}
