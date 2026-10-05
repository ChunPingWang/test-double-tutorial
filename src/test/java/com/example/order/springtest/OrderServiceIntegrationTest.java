package com.example.order.springtest;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.port.PaymentGateway;
import com.example.order.port.PaymentResult;
import com.example.order.service.OrderService;
import com.example.order.service.PlaceOrderCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 【Spring 整合測試 + 局部替身】
 *
 * <p>啟動完整 Spring 容器,大部分 bean 都用真的
 * (記憶體 Repository、庫存服務、通知服務),
 * 只把「最不該在測試中碰的外部依賴」—— 金流 —— 用
 * {@code @MockitoBean} 換成替身。
 *
 * <p>這是實務上最常見的折衷:整合測試覆蓋 bean 裝配與真實協作,
 * 但不打真的外部 API。
 */
@SpringBootTest
class OrderServiceIntegrationTest {

    @Autowired
    OrderService orderService;          // 真的 Service + 真的 Repository / Inventory / Notification

    @MockitoBean
    PaymentGateway paymentGateway;      // 只有金流被換成 Mockito mock

    @Test
    @DisplayName("整條下單流程走真實 bean,只有金流是替身")
    void placeOrder_withRealBeansExceptPayment() {
        when(paymentGateway.charge(anyString(), any(BigDecimal.class)))
                .thenReturn(PaymentResult.success("TXN-IT-1"));

        Order order = orderService.placeOrder(
                new PlaceOrderCommand("SKU-KEYBOARD", 2, new BigDecimal("1500")));

        // 狀態驗證:走過真實的 Repository,查得回來
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderService.getOrder(order.getId())).isPresent();

        // 行為驗證:金流被以正確金額呼叫
        verify(paymentGateway).charge(anyString(), org.mockito.ArgumentMatchers.eq(new BigDecimal("3000")));
    }
}
