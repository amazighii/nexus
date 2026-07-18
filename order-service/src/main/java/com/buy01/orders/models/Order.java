package com.buy01.orders.models;

import java.util.Date;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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
    @Field("phone_number")
    String phoneNumber;

    @NonNull
    String address;

    @NonNull
    @Field("client_id")
    String clientId;

    @NonNull
    OrderStatus status;

    @NonNull
    Boolean removed;

    @NonNull
    Long quantity;

    @NonNull
    Double price;

    @NonNull
    @Field("payment_method")
    OrderPaymentMethod paymentMethod;

    @NonNull
    Date date;
}
