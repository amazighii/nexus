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

    List<ProductOrder> productIds = new ArrayList<>();
}
