package com.example.order.stub;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.doubles.FakeOrderRepository;
import com.example.order.doubles.SpyNotificationService;
import com.example.order.doubles.StubInventoryClient;
import com.example.order.doubles.StubPaymentGateway;
import com.example.order.service.OrderService;
import com.example.order.service.PlaceOrderCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【Stub 示範】
 *
 * <p>Stub 的職責:控制 SUT 的「間接輸入」。
 * 金流回什麼、庫存夠不夠,都由測試事先寫死,
 * 然後用<b>狀態驗證</b>(檢查回傳值/最終狀態)確認結果。
 * 注意:整個測試沒有半句 verify —— 這就是 Stub 與 Mock 的分水嶺。
 */
class StubTest {

    @Test
    @DisplayName("庫存足夠且付款成功 → 訂單成立(CONFIRMED),金額與交易編號正確")
    void placeOrder_success_stateVerification() {
        var service = new OrderService(
                new FakeOrderRepository(),
                StubPaymentGateway.alwaysSucceed("TXN-12345"),  // Stub:寫死「付款成功」
                StubInventoryClient.alwaysInStock(),             // Stub:寫死「有貨」
                new SpyNotificationService());                   // 此測試不關心通知,給個無害的實作

        Order order = service.placeOrder(
                new PlaceOrderCommand("SKU-MONITOR", 2, new BigDecimal("8000")));

        // 狀態驗證:只看結果,不管 SUT 內部怎麼跟協作者互動
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("16000"); // 8000 x 2
        assertThat(order.getTransactionId()).isEqualTo("TXN-12345");
    }

    @Test
    @DisplayName("同一套商業邏輯,換一顆 Stub 就能測另一個世界(付款失敗)")
    void sameLogic_differentStub_differentWorld() {
        var service = new OrderService(
                new FakeOrderRepository(),
                StubPaymentGateway.alwaysFail("卡片已過期"),     // 只換這顆 Stub
                StubInventoryClient.alwaysInStock(),
                new SpyNotificationService());

        // 真實金流很難「叫它失敗一次」,Stub 讓失敗情境變得廉價、穩定、可重現
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.placeOrder(
                        new PlaceOrderCommand("SKU-MONITOR", 1, new BigDecimal("8000"))))
                .hasMessageContaining("卡片已過期");
    }
}
