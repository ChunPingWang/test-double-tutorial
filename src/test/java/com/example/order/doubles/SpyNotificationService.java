package com.example.order.doubles;

import com.example.order.domain.Order;
import com.example.order.port.NotificationService;

import java.util.ArrayList;
import java.util.List;

/**
 * 【Spy】會「錄音」的通知服務。
 *
 * <p>Spy 負責記錄 SUT 對它做了什麼(間接輸出, indirect output),
 * 但<b>不自己做判斷</b>——事後由測試程式讀取紀錄來斷言。
 * 這和 Mock 的差別在於:驗證邏輯寫在測試裡(Spy),
 * 還是內建在替身裡(Mock)。
 *
 * <p>注意:這是 Meszaros 定義的「手寫 Spy」;
 * Mockito 的 {@code @Spy} 是另一回事(對真實物件做部分代理),見 README。
 */
public class SpyNotificationService implements NotificationService {

    private final List<Order> receivedOrders = new ArrayList<>();

    @Override
    public void sendOrderConfirmation(Order order) {
        receivedOrders.add(order);
    }

    public int sentCount() {
        return receivedOrders.size();
    }

    public List<Order> receivedOrders() {
        return List.copyOf(receivedOrders);
    }

    public Order lastOrder() {
        if (receivedOrders.isEmpty()) {
            throw new IllegalStateException("尚未收到任何通知");
        }
        return receivedOrders.get(receivedOrders.size() - 1);
    }
}
