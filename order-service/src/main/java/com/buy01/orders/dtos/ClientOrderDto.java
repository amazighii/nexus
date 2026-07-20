package com.buy01.orders.dtos;

import java.util.List;

import com.buy01.orders.models.ProductRef;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientOrderDto {
    String id;

    // String firstname;

    // String lastname;

    // String phoneNumber;

    // String address;

    String clientId;

    String status;

    Double totalPrice;

    String paymentMethod;

    List<ProductRef> products;

    String date;
}
