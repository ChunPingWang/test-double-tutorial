package com.example.order.doubles;

import com.example.order.domain.Order;
import com.example.order.port.NotificationService;

/**
 * 【Dummy】佔位用的通知服務。
 *
 * <p>Dummy 的唯一任務是「把參數填滿」,讓 SUT 能被建構出來;
 * 測試情境中它<b>不應該被呼叫到</b>。
 * 這裡刻意在被呼叫時丟出例外——如果測試因此失敗,
 * 代表這個情境其實會用到通知服務,你選錯 double 了。
 */
public class DummyNotificationService implements NotificationService {

    @Override
    public void sendOrderConfirmation(Order order) {
        throw new UnsupportedOperationException(
                "Dummy 不應該被呼叫!此測試情境若會寄送通知,請改用 Spy 或 Mock。");
    }
}
