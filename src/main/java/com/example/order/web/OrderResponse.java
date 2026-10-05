package com.example.order.web;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;

import java.math.BigDecimal;

/** 訂單 API 的回應格式。 */
public record OrderResponse(String id,
                            String productId,
                            int quantity,
                            BigDecimal totalAmount,
                            OrderStatus status,
                            String transactionId) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getTransactionId());
    }
}
