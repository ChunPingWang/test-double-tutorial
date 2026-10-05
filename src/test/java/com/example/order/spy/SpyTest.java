package com.example.order.spy;

import com.example.order.domain.Order;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 【Spy 示範】
 *
 * <p>「寄送通知」是 void 方法,SUT 的回傳值完全看不出它有沒有發生——
 * 這種「間接輸出」就需要 Spy 把呼叫記錄下來,事後由測試翻閱紀錄來斷言。
 *
 * <p>Spy 和 Mock 都在驗證互動(行為驗證),差別在於:
 * Spy 只錄不判,斷言寫在測試裡;Mock 把期望與驗證內建在替身框架裡。
 */
class SpyTest {

    private final SpyNotificationService spyNotification = new SpyNotificationService();

    private OrderService serviceWith(StubPaymentGateway payment) {
        return new OrderService(
                new FakeOrderRepository(),
                payment,
                StubInventoryClient.alwaysInStock(),
                spyNotification);
    }

    @Test
    @DisplayName("下單成功 → 應寄出一封訂單確認通知,內容對應該筆訂單")
    void successfulOrder_sendsExactlyOneConfirmation() {
        var service = serviceWith(StubPaymentGateway.alwaysSucceed("TXN-777"));

        Order order = service.placeOrder(
                new PlaceOrderCommand("SKU-KEYBOARD", 1, new BigDecimal("1500")));

        // 翻閱 Spy 的紀錄,用一般的 assert 來驗證互動
        assertThat(spyNotification.sentCount()).isEqualTo(1);
        assertThat(spyNotification.lastOrder().getId()).isEqualTo(order.getId());
        assertThat(spyNotification.lastOrder().getTotalAmount()).isEqualByComparingTo("1500");
    }

    @Test
    @DisplayName("付款失敗 → 不應該寄出任何通知(驗證「沒有發生」的互動)")
    void failedPayment_sendsNothing() {
        var service = serviceWith(StubPaymentGateway.alwaysFail("餘額不足"));

        assertThatThrownBy(() -> service.placeOrder(
                new PlaceOrderCommand("SKU-KEYBOARD", 1, new BigDecimal("1500"))))
                .hasMessageContaining("餘額不足");

        // 「不該寄卻寄了」是真實世界常見的 bug(客戶收到確認信但沒成立訂單)
        assertThat(spyNotification.sentCount()).isZero();
    }
}
