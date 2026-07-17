package com.buy01.orders.models;

import java.util.Date;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mongodb.lang.NonNull;

import lombok.AllArgsConstructor;
import lombok.Data;

@Document(collection = "order")
@Data
@AllArgsConstructor
public class Order {

    @Indexed
    String Id;

    @NonNull
    String firstname;

    @NonNull
    String lastname;

    @NonNull
    String phone_number;

    @NonNull
    String address;

    @NonNull
    String client_id;

    @NonNull
    OrderStatus status;

    @NonNull
    Boolean removed;

    @NonNull
    Long quantity;

    @NonNull
    Double price;

    @NonNull
    OrderPaymentMethod payment_method;

    @NonNull
    Date date;
}
