package com.example.order.exception;

/** 金流扣款失敗。 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String reason) {
        super("付款失敗: " + reason);
    }
}
