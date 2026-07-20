package com.buy01.orders.models;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.mongodb.lang.NonNull;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Document(collection = "order")
@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    String id;

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
    @Field("is_removed")
    Boolean isRemoved;

    // @NonNull
    // Long quantity;

    @NonNull
    @Field("total_price")
    Double totalPrice;

    @NonNull
    @Field("payment_method")
    OrderPaymentMethod paymentMethod;

    @NonNull
    List<ProductRef> products;

    // @NonNull
    // @Field("seller_ids")
    // List<String> sellerIds;

    @NonNull
    Date date;
}
