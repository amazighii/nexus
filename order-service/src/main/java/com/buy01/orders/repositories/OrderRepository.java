package com.buy01.orders.repositories;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.buy01.orders.models.Order;
import com.buy01.orders.models.OrderStatus;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findOrdersByClientIdAndIsRemovedFalse(String userId);

    List<Order> findOrdersBySellerIdAndIsRemovedFalse(String sellerId);

    List<Order> findOrdersByStatusAndDateAndClientId(OrderStatus status, Date date, String clientId);

}