package com.buy01.orders.dtos;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ClientOrdersList {
    List<ClientOrderDto> clientOrders = new ArrayList<>();
}
