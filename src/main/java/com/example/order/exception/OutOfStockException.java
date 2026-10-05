package com.example.order.exception;

/** 庫存不足。 */
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String productId) {
        super("商品庫存不足: " + productId);
    }
}
