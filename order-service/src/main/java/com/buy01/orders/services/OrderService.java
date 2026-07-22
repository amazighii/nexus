package com.buy01.orders.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.bson.Document;
import org.springframework.data.domain.Sort;
// import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.buy01.orders.dtos.ClientOrderDto;
import com.buy01.orders.dtos.ClientOrdersList;
import com.buy01.orders.dtos.CreateOrderDto;
import com.buy01.orders.dtos.CreateOrderMessage;
import com.buy01.orders.dtos.AnalyticsPointDto;
import com.buy01.orders.dtos.DashboardAnalyticsDto;
import com.buy01.orders.dtos.ProductOrder;
import com.buy01.orders.dtos.ReturnMessage;
import com.buy01.orders.dtos.TopProductDto;
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

    public ClientOrdersList getSellerOrders(String sellerId, String userRole) {
        if (!"SELLER".equals(userRole)) {
            throw new ForbiddenAction("This action is forbidden");
        }

        Aggregation aggregation = Aggregation.newAggregation(
                // 1. Filter active orders for this seller
                Aggregation.match(
                        Criteria.where("is_removed").is(false)
                                .and("products.sellerId").is(sellerId)),

                // 2. Keep the entire Order object as-is, just overwrite 'products' with the
                // filtered list
                Aggregation.addFields()
                        .addFieldWithValue(
                                "products",
                                ArrayOperators.Filter.filter("products")
                                        .as("product")
                                        .by(ComparisonOperators.valueOf("$$product.sellerId").equalToValue(sellerId)))
                        .build());

        List<Order> orders = mongoTemplate.aggregate(aggregation, Order.class, Order.class).getMappedResults();

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
                    order.getTotalPrice(),
                    order.getPaymentMethod().toString(),
                    order.getProducts(),
                    order.getDate().toString());
            clientOrdersList.getClientOrders().add(clientOrderDto);
        });

        return clientOrdersList;
    }

    public CreateOrderMessage createOrder(String userId, CreateOrderDto createOrderDto) {
        Order order = new Order();
        List<ProductRef> products = new ArrayList<>();
        Double totalPrice = 0.0;

        List<ProductOrder> requestedProducts = createOrderDto.requestedProducts();
        if (requestedProducts == null || requestedProducts.isEmpty()) {
            throw new BadRequest("Products cannot be null or empty");
        }

        for (ProductOrder productOrder : requestedProducts) {
            if (productOrder.getProductId() == null || productOrder.getProductId().isBlank() || productOrder.getQuantity() == null || productOrder.getQuantity() < 1) {
                throw new BadRequest("Each order product must have a product ID and a quantity of at least one.");
            }
            ProductRef productRef = productRefRepository
                    .findByProductId(productOrder.getProductId())
                    .orElseThrow(() -> new BadRequest("Product not found"));
            if (userId.equals(productRef.getSellerId())) {
                throw new BadRequest("Sellers cannot purchase their own products.");
            }

            BigDecimal productPrice = productRef.getPrice().multiply(BigDecimal.valueOf(productOrder.getQuantity()));

            totalPrice += productPrice.doubleValue();

            // Store a snapshot in the order. Do not mutate the shared product reference.
            products.add(new ProductRef(
                    productRef.getProductId(),
                    productRef.getSellerId(),
                    productRef.getProductName(),
                    productRef.getPrice(),
                    productOrder.getQuantity(),
                    productRef.getDescription(),
                    productRef.getImageUrl()));

        }

        order.setFirstname(createOrderDto.getFirstname());
        order.setLastname(createOrderDto.getLastname());
        order.setPhoneNumber(createOrderDto.getPhoneNumber());
        order.setAddress(createOrderDto.getAddress());
        order.setClientId(userId);
        order.setProducts(products);
        order.setDate(new java.util.Date());
        order.setIsRemoved(false);
        order.setTotalPrice(totalPrice);

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

    public ReturnMessage cancelOrder(String userId, String userRole, String orderId) {
        if (!"CLIENT".equals(userRole)) {
            throw new ForbiddenAction("Only clients can cancel orders.");
        }

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

    public ClientOrdersList searchOrders(String userId, String userRole, String status, Date date, String view) {

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

        String normalizedView = view == null ? "client" : view.trim().toLowerCase();
        OrderStatus requestedStatus = parseStatus(status);

        if ("seller".equals(normalizedView)) {
            if (!"SELLER".equals(userRole)) {
                throw new ForbiddenAction("This action is forbidden");
            }

            Criteria criteria = Criteria.where("is_removed").is(false)
                    .and("products.sellerId").is(userId);
            if (requestedStatus != null) {
                criteria = criteria.and("status").is(requestedStatus);
            }
            if (startDate != null && endDate != null) {
                criteria = criteria.and("date").gte(startDate).lte(endDate);
            }

            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(criteria),
                    Aggregation.addFields()
                            .addFieldWithValue(
                                    "products",
                                    ArrayOperators.Filter.filter("products")
                                            .as("product")
                                            .by(ComparisonOperators.valueOf("$$product.sellerId").equalToValue(userId)))
                            .build());

            List<Order> orders = mongoTemplate.aggregate(aggregation, Order.class, Order.class).getMappedResults();
            return constructClientOrderDto(orders);
        }

        if (!"client".equals(normalizedView)) {
            throw new BadRequest("Unsupported order search view: " + view);
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("clientId").is(userId));
        query.addCriteria(Criteria.where("is_removed").is(false));

        if (requestedStatus != null) {
            query.addCriteria(Criteria.where("status").is(requestedStatus));
        }

        if (startDate != null && endDate != null) {
            query.addCriteria(Criteria.where("date").gte(startDate).lte(endDate));
        }

        List<Order> orders = mongoTemplate.find(query, Order.class);

        return constructClientOrderDto(orders);

    }

    public List<TopProductDto> getClientBestProducts(String clientId, Long limit) {
        return aggregateClientProducts(clientId, limit, "totalSpent");
    }

    public List<TopProductDto> getClientMostBuyingProducts(String clientId, Long limit) {
        return aggregateClientProducts(clientId, limit, "totalQuantity");
    }

    public List<TopProductDto> getSellerBestSellingProducts(String sellerId, Long limit) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("is_removed").is(false)
                                .and("status").is(OrderStatus.DELIVERED.name())
                                .and("products.sellerId").is(sellerId)),

                Aggregation.unwind("products"),
                Aggregation.match(Criteria.where("products.sellerId").is(sellerId)),

                Aggregation.group("products._id")
                        .first("products.productName").as("productName")
                        .first("products.imageUrl").as("imageUrl")
                        .sum("products.quantity").as("totalQuantity")
                        .sum(calculateItemTotal()).as("totalSpent"),

                Aggregation.project()
                        .and("_id").as("productId")
                        .and("productName").as("productName")
                        .and("imageUrl").as("imageUrl")
                        .and("totalQuantity").as("totalQuantity")
                        .and("totalSpent").as("totalSpent"),

                Aggregation.sort(Sort.Direction.DESC, "totalSpent"),
                Aggregation.limit(limit));

        AggregationResults<TopProductDto> results = mongoTemplate.aggregate(
                aggregation,
                Order.class,
                TopProductDto.class);

        return results.getMappedResults();
    }

    public DashboardAnalyticsDto getClientDashboard(String clientId, Long limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("client_id").is(clientId)
                .and("is_removed").is(false)
                .and("status").is(OrderStatus.DELIVERED.name()));
        query.with(Sort.by(Sort.Direction.ASC, "date"));

        List<Order> orders = mongoTemplate.find(query, Order.class);
        BigDecimal totalSpent = orders.stream()
                .map(order -> BigDecimal.valueOf(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardAnalyticsDto(
                totalSpent,
                getClientMostBuyingProducts(clientId, limit),
                buildClientHistory(orders));
    }

    public DashboardAnalyticsDto getSellerDashboard(String sellerId, Long limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("is_removed").is(false)
                .and("status").is(OrderStatus.DELIVERED.name())
                .and("products.sellerId").is(sellerId));
        query.with(Sort.by(Sort.Direction.ASC, "date"));

        List<Order> orders = mongoTemplate.find(query, Order.class);
        List<AnalyticsPointDto> history = buildSellerHistory(orders, sellerId);
        BigDecimal totalRevenue = history.stream()
                .map(AnalyticsPointDto::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardAnalyticsDto(
                totalRevenue,
                getSellerBestSellingProducts(sellerId, limit),
                history);
    }

    private List<TopProductDto> aggregateClientProducts(String clientId, Long limit, String sortField) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("client_id").is(clientId)
                                .and("is_removed").is(false)
                                .and("status").is(OrderStatus.DELIVERED.name())),
                Aggregation.unwind("products"),
                Aggregation.group("products._id")
                        .first("products.productName").as("productName")
                        .first("products.imageUrl").as("imageUrl")
                        .sum("products.quantity").as("totalQuantity")
                        .sum(calculateItemTotal()).as("totalSpent"),
                Aggregation.project()
                        .and("_id").as("productId")
                        .and("productName").as("productName")
                        .and("imageUrl").as("imageUrl")
                        .and("totalQuantity").as("totalQuantity")
                        .and("totalSpent").as("totalSpent"),
                Aggregation.sort(Sort.Direction.DESC, sortField),
                Aggregation.limit(limit));

        return mongoTemplate.aggregate(aggregation, Order.class, TopProductDto.class).getMappedResults();
    }

    private List<AnalyticsPointDto> buildClientHistory(List<Order> orders) {
        Map<String, BigDecimal> totalsByDay = new LinkedHashMap<>();
        for (Order order : orders) {
            String day = formatDay(order.getDate());
            BigDecimal amount = BigDecimal.valueOf(order.getTotalPrice());
            totalsByDay.merge(day, amount, BigDecimal::add);
        }
        return totalsByDay.entrySet().stream()
                .map(entry -> new AnalyticsPointDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<AnalyticsPointDto> buildSellerHistory(List<Order> orders, String sellerId) {
        Map<String, BigDecimal> totalsByDay = new LinkedHashMap<>();
        for (Order order : orders) {
            BigDecimal sellerTotal = order.getProducts().stream()
                    .filter(product -> sellerId.equals(product.getSellerId()))
                    .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sellerTotal.compareTo(BigDecimal.ZERO) > 0) {
                totalsByDay.merge(formatDay(order.getDate()), sellerTotal, BigDecimal::add);
            }
        }
        return totalsByDay.entrySet().stream()
                .map(entry -> new AnalyticsPointDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String formatDay(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }

    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequest("Invalid order status: " + status);
        }
    }

    private AggregationExpression calculateItemTotal() {
        return new AggregationExpression() {
            @Override
            public Document toDocument(AggregationOperationContext context) {
                return new Document("$multiply", Arrays.asList(
                        new Document("$toDouble", new Document("$ifNull", Arrays.asList("$products.price", 0.0))),
                        "$products.quantity"));
            }
        };
    }

}
