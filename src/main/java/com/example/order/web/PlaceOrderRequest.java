package com.example.order.web;

import java.math.BigDecimal;

/** POST /api/orders 的請求格式。 */
public record PlaceOrderRequest(String productId, int quantity, BigDecimal unitPrice) {
}
