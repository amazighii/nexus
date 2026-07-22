package com.buy01.orders.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.buy01.orders.dtos.ClientOrdersList;
import com.buy01.orders.dtos.CreateOrderDto;
import com.buy01.orders.dtos.CreateOrderMessage;
import com.buy01.orders.dtos.ProductOrder;
import com.buy01.orders.dtos.ReturnMessage;
import com.buy01.orders.exception.BadRequest;
import com.buy01.orders.exception.ForbiddenAction;
import com.buy01.orders.models.Order;
import com.buy01.orders.models.OrderPaymentMethod;
import com.buy01.orders.models.OrderStatus;
import com.buy01.orders.models.ProductRef;
import com.buy01.orders.repositories.OrderRepository;
import com.buy01.orders.repositories.ProductRefRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRefRepository productRefRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    void getClientOrders_ShouldReturnOrders() {
        Order order = createOrder();

        when(orderRepository.findOrdersByClientIdAndIsRemovedFalse("client-1"))
                .thenReturn(List.of(order));

        ClientOrdersList response = orderService.getClientOrders("client-1");

        assertEquals(1, response.getClientOrders().size());
        assertEquals(order.getId(), response.getClientOrders().get(0).getId());
        assertEquals(order.getFirstname(), response.getClientOrders().get(0).getFirstname());
        assertEquals(order.getLastname(), response.getClientOrders().get(0).getLastname());
    }

    @Test
    void getClientOrders_ShouldReturnEmptyList_WhenNoOrdersExist() {

        when(orderRepository.findOrdersByClientIdAndIsRemovedFalse("client-1"))
                .thenReturn(List.of());

        ClientOrdersList response = orderService.getClientOrders("client-1");

        assertTrue(response.getClientOrders().isEmpty());
    }

    @Test
    void createOrder_ShouldSaveOrder() {

        ProductRef product = createProductRef();

        when(productRefRepository.findByProductId("product-1"))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderMessage message = orderService.createOrder("client-1", createOrderDto());

        assertEquals("Order created successfully", message.getMessage());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(captor.capture());

        Order saved = captor.getValue();

        assertEquals("John", saved.getFirstname());
        assertEquals("Doe", saved.getLastname());
        assertEquals("client-1", saved.getClientId());
        assertEquals(OrderStatus.PENDING, saved.getStatus());
        assertEquals(OrderPaymentMethod.PAY_ON_DELIVERY, saved.getPaymentMethod());
        assertFalse(saved.getIsRemoved());
        assertEquals(1, saved.getProducts().size());
        assertEquals(40.0, saved.getTotalPrice());
    }

    @Test
    void createOrder_ShouldThrowBadRequest_WhenProductsAreEmpty() {

        CreateOrderDto dto = new CreateOrderDto();

        dto.setFirstname("John");
        dto.setLastname("Doe");
        dto.setPhoneNumber("0600000000");
        dto.setAddress("Oujda");
        dto.setPaymentMethod("PAY_ON_DELIVERY");

        assertThrows(BadRequest.class,
                () -> orderService.createOrder("client-1", dto));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_ShouldThrowBadRequest_WhenProductDoesNotExist() {

        when(productRefRepository.findByProductId("product-1"))
                .thenReturn(Optional.empty());

        assertThrows(BadRequest.class,
                () -> orderService.createOrder("client-1", createOrderDto()));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_ShouldThrowBadRequest_WhenSellerBuysOwnProduct() {

        ProductRef product = createProductRef();
        product.setSellerId("client-1");

        when(productRefRepository.findByProductId("product-1"))
                .thenReturn(Optional.of(product));

        assertThrows(BadRequest.class,
                () -> orderService.createOrder("client-1", createOrderDto()));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void removeOrder_ShouldMarkOrderAsRemoved() {

        Order order = createOrder();

        when(orderRepository.findById("order-1"))
                .thenReturn(Optional.of(order));

        ReturnMessage response = orderService.removeOrder("client-1", "order-1");

        assertEquals("Order removed successfully", response.getMessage());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(captor.capture());

        assertTrue(captor.getValue().getIsRemoved());
    }

    @Test
    void removeOrder_ShouldThrowForbidden_WhenUserDoesNotOwnOrder() {

        Order order = createOrder();
        order.setClientId("another-client");

        when(orderRepository.findById("order-1"))
                .thenReturn(Optional.of(order));

        assertThrows(ForbiddenAction.class,
                () -> orderService.removeOrder("client-1", "order-1"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_ShouldCancelPendingOrder() {

        Order order = createOrder();

        when(orderRepository.findById("order-1"))
                .thenReturn(Optional.of(order));

        ReturnMessage response = orderService.cancelOrder("client-1", "CLIENT", "order-1");

        assertEquals("Order cancelled successfully", response.getMessage());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(captor.capture());

        assertEquals(OrderStatus.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    void cancelOrder_ShouldThrowForbidden_WhenUserIsNotClient() {

        assertThrows(ForbiddenAction.class,
                () -> orderService.cancelOrder("client-1", "SELLER", "order-1"));

        verify(orderRepository, never()).findById(any());
    }

    @Test
    void cancelOrder_ShouldThrowForbidden_WhenUserDoesNotOwnOrder() {

        Order order = createOrder();
        order.setClientId("another-client");

        when(orderRepository.findById("order-1"))
                .thenReturn(Optional.of(order));

        assertThrows(ForbiddenAction.class,
                () -> orderService.cancelOrder("client-1", "CLIENT", "order-1"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_ShouldThrowBadRequest_WhenOrderIsNotPending() {

        Order order = createOrder();
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById("order-1"))
                .thenReturn(Optional.of(order));

        assertThrows(BadRequest.class,
                () -> orderService.cancelOrder("client-1", "CLIENT", "order-1"));

        verify(orderRepository, never()).save(any());
    }

    private CreateOrderDto createOrderDto() {

        ProductOrder productOrder = new ProductOrder();
        productOrder.setProductId("product-1");
        productOrder.setQuantity(2L);

        CreateOrderDto dto = new CreateOrderDto();
        dto.setFirstname("John");
        dto.setLastname("Doe");
        dto.setPhoneNumber("0600000000");
        dto.setAddress("Oujda");
        dto.setPaymentMethod("PAY_ON_DELIVERY");
        dto.setProducts(List.of(productOrder));

        return dto;
    }

    private ProductRef createProductRef() {

        ProductRef product = new ProductRef();

        product.setProductId("product-1");
        product.setSellerId("seller-1");
        product.setProductName("Gaming Mouse");
        product.setPrice(new BigDecimal("20.00"));
        product.setQuantity(10);
        product.setDescription("Wireless gaming mouse");
        product.setImageUrl("mouse.png");

        return product;
    }

    private Order createOrder() {

        Order order = new Order();

        order.setId("order-1");
        order.setFirstname("John");
        order.setLastname("Doe");
        order.setPhoneNumber("0600000000");
        order.setAddress("Oujda");
        order.setClientId("client-1");
        order.setStatus(OrderStatus.PENDING);
        order.setIsRemoved(false);
        order.setTotalPrice(40.0);
        order.setPaymentMethod(OrderPaymentMethod.PAY_ON_DELIVERY);
        order.setProducts(List.of(createProductRef()));
        order.setDate(new Date());

        return order;
    }
}