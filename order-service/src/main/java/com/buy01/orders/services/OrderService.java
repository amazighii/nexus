package com.buy01.orders.services;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.buy01.orders.dtos.ClientOrderDto;
import com.buy01.orders.dtos.ClientOrdersList;
import com.buy01.orders.dtos.CreateOrderDto;
import com.buy01.orders.dtos.CreateOrderMessage;
import com.buy01.orders.dtos.ReturnMessage;
import com.buy01.orders.exception.BadRequest;
import com.buy01.orders.exception.ForbiddenAction;
import com.buy01.orders.exception.OrderNotFound;
import com.buy01.orders.models.Order;
import com.buy01.orders.models.OrderPaymentMethod;
import com.buy01.orders.models.OrderStatus;
import com.buy01.orders.models.ProductRef;
import com.buy01.orders.repositories.OrderRepository;
import com.buy01.orders.repositories.ProductRefRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRefRepository productRefRepository;
    private final MongoTemplate mongoTemplate;

    public ClientOrdersList getClientOrders(String userId) {

        List<Order> orders = orderRepository.findOrdersByClientIdAndIsRemovedFalse(userId);

        return constructClientOrderDto(orders);
    }

    public ClientOrdersList getSellerOrders(String userId, String userRole) {
        if (!userRole.equals("SELLER")) {
            throw new ForbiddenAction("This action is forbidden");
        }

        List<Order> orders = orderRepository.findOrdersBySellerIdAndIsRemovedFalse(userId);

        return constructClientOrderDto(orders);
    }

    private ClientOrdersList constructClientOrderDto(List<Order> orders) {
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

        ProductRef productRef = productRefRepository
                .findByProductId(createOrderDto.getProductId())
                .orElseThrow(() -> new BadRequest("Product not found"));

        BigDecimal productPrice = productRef.getPrice().multiply(BigDecimal.valueOf(createOrderDto.getQuantity()));
        Double price = productPrice.doubleValue();

        order.setFirstname(createOrderDto.getFirstname());
        order.setLastname(createOrderDto.getLastname());
        order.setPhoneNumber(createOrderDto.getPhoneNumber());
        order.setAddress(createOrderDto.getAddress());
        order.setClientId(userId);
        order.setQuantity(createOrderDto.getQuantity());
        order.setProductId(createOrderDto.getProductId());
        order.setDate(new java.util.Date());
        order.setIsRemoved(false);
        order.setPrice(price);
        order.setSellerId(productRef.getSellerId());

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

    public ReturnMessage removeOrder(String userId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFound());

        if (!order.getClientId().equals(userId)) {
            throw new ForbiddenAction("You are not authorized to remove this order");
        }

        order.setIsRemoved(true);

        orderRepository.save(order);

        return new ReturnMessage("Order removed successfully");
    }

    public ReturnMessage cancelOrder(String userId, String orderId) {
        System.out.println("Cancelling order with ID: " + orderId + " for user: " + userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFound());

        if (!order.getClientId().equals(userId)) {
            throw new ForbiddenAction("You are not authorized to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequest("Only pending orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);

        return new ReturnMessage("Order cancelled successfully");
    }

    public ClientOrdersList searchOrders(String userId, String status, Date date) {

        Date startDate = null;
        Date endDate = null;

        if (date != null) {
            startDate = date; // 00:00:00
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            endDate = cal.getTime(); // 23:59:59
        }

        Query query = new Query();

        query.addCriteria(Criteria.where("clientId").is(userId));

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        if (startDate != null && endDate != null) {
            query.addCriteria(Criteria.where("date").gte(startDate).lte(endDate));
        }

        List<Order> orders = mongoTemplate.find(query, Order.class);

        return constructClientOrderDto(orders);

    }

}
