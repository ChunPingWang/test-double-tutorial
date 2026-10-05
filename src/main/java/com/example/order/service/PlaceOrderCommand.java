package com.example.order.service;

import java.math.BigDecimal;

/** 下訂單的輸入參數。 */
public record PlaceOrderCommand(String productId, int quantity, BigDecimal unitPrice) {
}
