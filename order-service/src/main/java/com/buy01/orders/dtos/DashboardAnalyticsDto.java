package com.buy01.orders.dtos;

import java.math.BigDecimal;
import java.util.List;

public record DashboardAnalyticsDto(
        BigDecimal totalAmount,
        List<TopProductDto> topProducts,
        List<AnalyticsPointDto> history) {
}
