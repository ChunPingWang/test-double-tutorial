package com.example.order.port;

/**
 * 金流扣款結果。
 */
public record PaymentResult(boolean success, String transactionId, String failureReason) {

    public static PaymentResult success(String transactionId) {
        return new PaymentResult(true, transactionId, null);
    }

    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
