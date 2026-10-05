package com.example.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 訂單(領域物件)。
 *
 * <p>注意:這是「值/實體物件」,在測試中應直接 new 出來使用,
 * 不應該為它建立 test double(見 README「常見反模式」)。
 */
public class Order {

    private final String id;
    private final String productId;
    private final int quantity;
    private final BigDecimal totalAmount;
    private OrderStatus status;
    private String transactionId;

    private Order(String id, String productId, int quantity, BigDecimal totalAmount) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
    }

    /** 建立一筆待付款訂單,總金額 = 單價 x 數量。 */
    public static Order create(String productId, int quantity, BigDecimal unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return new Order(UUID.randomUUID().toString(), productId, quantity, total);
    }

    /** 付款成功後,記錄金流交易編號並將訂單轉為 CONFIRMED。 */
    public void markPaid(String transactionId) {
        this.transactionId = transactionId;
        this.status = OrderStatus.CONFIRMED;
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
