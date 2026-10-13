package com.br.orderservice.dto.response;

import com.br.orderservice.model.CustomerOrder;
import com.br.orderservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerOrderResponse(
        Long id,
        Long dishId,
        int quantity,
        BigDecimal totalPrice,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static CustomerOrderResponse from(CustomerOrder order) {
        return new CustomerOrderResponse(
                order.getId(),
                order.getDishId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}