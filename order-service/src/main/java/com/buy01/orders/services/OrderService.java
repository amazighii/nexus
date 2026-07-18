package com.buy01.orders.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.buy01.orders.dtos.ClientOrderDto;
import com.buy01.orders.dtos.ClientOrdersList;
import com.buy01.orders.dtos.CreateOrderDto;
import com.buy01.orders.dtos.CreateOrderMessage;
import com.buy01.orders.exception.BadRequest;
import com.buy01.orders.models.Order;
import com.buy01.orders.models.OrderPaymentMethod;
import com.buy01.orders.models.OrderStatus;
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

    public CreateOrderMessage createOrder(String userId, CreateOrderDto createOrderDto) {
        Order order = new Order();

        order.setFirstname(createOrderDto.getFirstname());
        order.setLastname(createOrderDto.getLastname());
        order.setPhoneNumber(createOrderDto.getPhoneNumber());
        order.setAddress(createOrderDto.getAddress());
        order.setClientId(userId);
        order.setQuantity(createOrderDto.getQuantity());
        order.setProductId(createOrderDto.getProductId());
        order.setDate(new java.util.Date());

        try {
            order.setStatus(OrderStatus.valueOf("PENDING"));
        } catch (IllegalArgumentException e) {
            throw new BadRequest("Invalid order status: " + order.getStatus());
        }

        try {
            order.setPaymentMethod(OrderPaymentMethod.valueOf(createOrderDto.getPaymentMethod()));
        } catch (IllegalArgumentException e) {
            throw new BadRequest("Invalid order payment method: " + createOrderDto.getPaymentMethod());
        }

        try {
            orderRepository.save(order);
        } catch (Exception e) {
            throw new BadRequest("Error occurred while saving order");
        }

        return new CreateOrderMessage("Order created successfully");
    }

}
