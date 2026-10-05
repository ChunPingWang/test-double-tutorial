package com.example.order.dummy;

import com.example.order.doubles.DummyNotificationService;
import com.example.order.doubles.FakeOrderRepository;
import com.example.order.doubles.StubInventoryClient;
import com.example.order.doubles.StubPaymentGateway;
import com.example.order.exception.OutOfStockException;
import com.example.order.exception.PaymentFailedException;
import com.example.order.port.PaymentGateway;
import com.example.order.service.OrderService;
import com.example.order.service.PlaceOrderCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 【Dummy 示範】
 *
 * <p>情境:測「付款失敗」與「庫存不足」。這兩條路徑根本走不到
 * 「寄送通知」那一步,但 OrderService 的建構子還是要求傳入
 * NotificationService —— 這時傳的就是 Dummy:純粹填參數,不會被用到。
 */
class DummyTest {

    @Test
    @DisplayName("付款失敗時應丟出 PaymentFailedException,且不會動用通知服務(Dummy)")
    void paymentFailure_notificationIsNeverUsed() {
        // DummyNotificationService 一旦被呼叫就會丟例外,
        // 測試通過本身就證明了:這條路徑確實用不到通知服務。
        var dummyNotification = new DummyNotificationService();
        var fakeRepository = new FakeOrderRepository();

        var service = new OrderService(
                fakeRepository,
                StubPaymentGateway.alwaysFail("餘額不足"),   // Stub:把 SUT 推進失敗分支
                StubInventoryClient.alwaysInStock(),
                dummyNotification);                           // Dummy:佔位而已

        assertThatThrownBy(() -> service.placeOrder(
                new PlaceOrderCommand("SKU-KEYBOARD", 2, new BigDecimal("1500"))))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("餘額不足");

        // 付款失敗的訂單不應該被存進去
        assertThat(fakeRepository.count()).isZero();
    }

    @Test
    @DisplayName("庫存不足時應丟出 OutOfStockException —— 金流與通知都只是 Dummy")
    void outOfStock_paymentAndNotificationAreDummies() {
        // 這個情境在「查庫存」就中斷了,金流也用不到,
        // 所以金流也可以是 Dummy。單方法介面用 lambda 寫 Dummy 最省事:
        PaymentGateway dummyPayment = (orderId, amount) -> {
            throw new UnsupportedOperationException("Dummy 不應該被呼叫");
        };

        var service = new OrderService(
                new FakeOrderRepository(),
                dummyPayment,
                StubInventoryClient.alwaysOutOfStock(),
                new DummyNotificationService());

        assertThatThrownBy(() -> service.placeOrder(
                new PlaceOrderCommand("SKU-LAPTOP", 99, new BigDecimal("45000"))))
                .isInstanceOf(OutOfStockException.class);
    }
}
