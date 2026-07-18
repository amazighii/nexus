package com.buy01.orders.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.buy01.orders.dtos.ClientOrderDto;
import com.buy01.orders.dtos.ClientOrdersList;
import com.buy01.orders.models.Order;
import com.buy01.orders.repositories.OrderRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public ClientOrdersList getClientOrders(String userId) {

        List<Order> orders = orderRepository.findOrdersByClientId(userId);

        ClientOrdersList clientOrdersList = new ClientOrdersList();

        orders.forEach(order -> {
            ClientOrderDto clientOrderDto = new ClientOrderDto(
                    order.getId(),
                    order.getFirstname(),
                    order.getLastname(),
                    order.getPhoneNumber(),
                    order.getAddress(),
                    order.getClientId(),
                    order.getStatus().toString(),
                    order.getQuantity(),
                    order.getPrice(),
                    order.getPaymentMethod().toString(),
                    order.getDate().toString());
            clientOrdersList.getClientOrders().add(clientOrderDto);
        });

        return clientOrdersList;
    }
}
