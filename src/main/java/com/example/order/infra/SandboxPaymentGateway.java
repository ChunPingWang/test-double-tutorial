package com.example.order.infra;

import com.example.order.port.PaymentGateway;
import com.example.order.port.PaymentResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 模擬的金流閘道(教學專案以此代替真實的第三方金流 API)。
 * 規則:單筆金額超過 100,000 視為超過刷卡額度,扣款失敗。
 */
@Component
public class SandboxPaymentGateway implements PaymentGateway {

    private static final BigDecimal LIMIT = new BigDecimal("100000");

    @Override
    public PaymentResult charge(String orderId, BigDecimal amount) {
        if (amount.compareTo(LIMIT) > 0) {
            return PaymentResult.failure("超過單筆刷卡額度");
        }
        return PaymentResult.success("TXN-" + UUID.randomUUID());
    }
}
