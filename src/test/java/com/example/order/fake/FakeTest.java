package com.example.order.fake;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【Fake 示範】
 *
 * <p>Fake 是介面的「真實作,但抄捷徑」:用 HashMap 代替資料庫。
 * 它的價值在於<b>行為一致性</b>——存進去的東西查得回來,
 * 所以能自然地測「跨呼叫」的流程(先下單、再查詢),
 * 而不必像 Mock/Stub 那樣預先編排每一次呼叫的回應。
 */
class FakeTest {

    @Test
    @DisplayName("下單後再查詢,查得到同一筆已成立的訂單(Fake 保證存取一致性)")
    void placedOrder_canBeFoundLater() {
        var fakeRepository = new FakeOrderRepository();
        var service = new OrderService(
                fakeRepository,
                StubPaymentGateway.alwaysSucceed("TXN-888"),
                StubInventoryClient.alwaysInStock(),
                new SpyNotificationService());

        // 第一次互動:下單
        Order placed = service.placeOrder(
                new PlaceOrderCommand("SKU-LAPTOP", 1, new BigDecimal("45000")));

        // 第二次互動:用下單取得的 id 查詢 —— Fake 讓這個流程「真的成立」
        Optional<Order> found = service.getOrder(placed.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(found.get().getTransactionId()).isEqualTo("TXN-888");

        // 若用 Stub 寫死 findById 的回傳值,這個測試就測不出「save 真的有存」
    }

    @Test
    @DisplayName("查詢不存在的訂單應回傳空值")
    void unknownOrder_returnsEmpty() {
        var service = new OrderService(
                new FakeOrderRepository(),
                StubPaymentGateway.alwaysSucceed("TXN-999"),
                StubInventoryClient.alwaysInStock(),
                new SpyNotificationService());

        assertThat(service.getOrder("no-such-id")).isEmpty();
    }
}
