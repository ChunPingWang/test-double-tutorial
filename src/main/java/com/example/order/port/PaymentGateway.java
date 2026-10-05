package com.example.order.port;

import java.math.BigDecimal;

/**
 * 第三方金流閘道(外部系統的抽象)。
 *
 * <p>真實實作會打外部 HTTP API:慢、不穩定、會真的扣款——
 * 這正是單元測試中必須用 test double 隔離的典型依賴。
 * 測試時常以 <b>Stub</b> 控制回傳結果(成功/失敗),
 * 或以 <b>Mock</b> 驗證「扣款金額是否正確」。
 */
public interface PaymentGateway {

    /**
     * 對指定訂單扣款。
     *
     * @param orderId 訂單編號(作為冪等鍵)
     * @param amount  扣款金額
     */
    PaymentResult charge(String orderId, BigDecimal amount);
}
