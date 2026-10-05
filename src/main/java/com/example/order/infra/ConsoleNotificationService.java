package com.example.order.infra;

import com.example.order.domain.Order;
import com.example.order.port.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 模擬的通知服務(教學專案以 log 代替真實的 Email / 簡訊發送)。
 */
@Component
public class ConsoleNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationService.class);

    @Override
    public void sendOrderConfirmation(Order order) {
        log.info("寄送訂單確認信: orderId={}, 商品={}, 金額={}",
                order.getId(), order.getProductId(), order.getTotalAmount());
    }
}
