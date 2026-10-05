package com.example.order.doubles;

import com.example.order.port.PaymentGateway;
import com.example.order.port.PaymentResult;

import java.math.BigDecimal;

/**
 * 【Stub】回傳預先設定結果的金流閘道。
 *
 * <p>真實金流又慢又會真的扣款,單元測試絕不能碰;
 * 用 stub 就能在一毫秒內模擬「扣款成功」與「餘額不足」兩種世界。
 */
public class StubPaymentGateway implements PaymentGateway {

    private final PaymentResult cannedResult;

    private StubPaymentGateway(PaymentResult cannedResult) {
        this.cannedResult = cannedResult;
    }

    /** 建立一個永遠扣款成功的 stub,回傳固定交易編號。 */
    public static StubPaymentGateway alwaysSucceed(String transactionId) {
        return new StubPaymentGateway(PaymentResult.success(transactionId));
    }

    /** 建立一個永遠扣款失敗的 stub。 */
    public static StubPaymentGateway alwaysFail(String reason) {
        return new StubPaymentGateway(PaymentResult.failure(reason));
    }

    @Override
    public PaymentResult charge(String orderId, BigDecimal amount) {
        return cannedResult;
    }
}
