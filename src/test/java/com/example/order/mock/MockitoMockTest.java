package com.example.order.mock;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.exception.PaymentFailedException;
import com.example.order.port.InventoryClient;
import com.example.order.port.NotificationService;
import com.example.order.port.OrderRepository;
import com.example.order.port.PaymentGateway;
import com.example.order.port.PaymentResult;
import com.example.order.service.OrderService;
import com.example.order.service.PlaceOrderCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 【Mock 示範(Mockito)】
 *
 * <p>Mock 做「行為驗證」:測試關心的不是回傳值,而是
 * 「SUT 有沒有用對的參數、對的次數、對的順序呼叫協作者」。
 *
 * <p>Mockito 的 {@code mock()} 其實是萬用替身:
 * 用 {@code when(...).thenReturn(...)} 設定回傳 → 它扮演 Stub;
 * 用 {@code verify(...)} 驗證互動 → 它扮演 Mock。
 * 一個測試裡兩者常常並用,本類別即是如此。
 */
@ExtendWith(MockitoExtension.class)
class MockitoMockTest {

    @Mock OrderRepository orderRepository;
    @Mock PaymentGateway paymentGateway;
    @Mock InventoryClient inventoryClient;
    @Mock NotificationService notificationService;

    OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, paymentGateway, inventoryClient, notificationService);
    }

    @Test
    @DisplayName("下單成功:驗證對每個協作者的互動(參數、次數、順序)")
    void placeOrder_success_behaviorVerification() {
        // ===== Arrange:先讓 mock 扮演 Stub,餵出走向成功路徑所需的回答 =====
        when(inventoryClient.isInStock("SKU-MONITOR", 2)).thenReturn(true);
        when(paymentGateway.charge(anyString(), any(BigDecimal.class)))
                .thenReturn(PaymentResult.success("TXN-MOCK-1"));
        // repository.save 原樣回傳傳入的訂單(常見技巧:thenAnswer 轉傳參數)
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // ===== Act =====
        service.placeOrder(new PlaceOrderCommand("SKU-MONITOR", 2, new BigDecimal("8000")));

        // ===== Assert:行為驗證 =====
        // 1. 扣款金額必須正確(8000 x 2 = 16000)—— 這是金流最不能錯的事
        verify(paymentGateway).charge(anyString(), eq(new BigDecimal("16000")));

        // 2. 庫存要扣對商品、對數量
        verify(inventoryClient).deduct("SKU-MONITOR", 2);

        // 3. 用 ArgumentCaptor 捕捉傳給通知服務的訂單,深入檢查其內容
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(notificationService).sendOrderConfirmation(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderCaptor.getValue().getTransactionId()).isEqualTo("TXN-MOCK-1");

        // 4. 關鍵順序:必須「先存訂單、再寄通知」,否則客戶可能收到查無此單的確認信
        InOrder inOrder = inOrder(orderRepository, notificationService);
        inOrder.verify(orderRepository).save(any(Order.class));
        inOrder.verify(notificationService).sendOrderConfirmation(any(Order.class));
    }

    @Test
    @DisplayName("付款失敗:不得扣庫存、不得存訂單、不得寄通知(驗證「沒發生」)")
    void paymentFailure_noSideEffects() {
        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(true);
        when(paymentGateway.charge(anyString(), any(BigDecimal.class)))
                .thenReturn(PaymentResult.failure("餘額不足"));

        assertThatThrownBy(() -> service.placeOrder(
                new PlaceOrderCommand("SKU-MONITOR", 1, new BigDecimal("8000"))))
                .isInstanceOf(PaymentFailedException.class);

        // 行為驗證的另一半威力:確認副作用「沒有」發生
        verify(inventoryClient, never()).deduct(anyString(), anyInt());
        verifyNoInteractions(orderRepository, notificationService);
    }

    @Test
    @DisplayName("用 thenThrow 模擬金流逾時:例外應往上拋,且不留下任何副作用")
    void paymentGatewayTimeout_exceptionPropagates() {
        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(true);
        // Stub 也能餵「例外」這種間接輸入 —— 真實金流很難叫它逾時一次給你測
        when(paymentGateway.charge(anyString(), any(BigDecimal.class)))
                .thenThrow(new IllegalStateException("connection timeout"));

        assertThatThrownBy(() -> service.placeOrder(
                new PlaceOrderCommand("SKU-MONITOR", 1, new BigDecimal("8000"))))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(orderRepository, notificationService);
    }
}
