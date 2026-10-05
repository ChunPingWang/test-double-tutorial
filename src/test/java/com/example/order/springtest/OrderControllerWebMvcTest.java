package com.example.order.springtest;

import com.example.order.domain.Order;
import com.example.order.exception.OutOfStockException;
import com.example.order.service.OrderService;
import com.example.order.service.PlaceOrderCommand;
import com.example.order.web.OrderController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【Spring 切片測試 + Mock】
 *
 * <p>{@code @WebMvcTest} 只載入 Web 層(Controller、轉換器、例外處理),
 * Service 層用 {@code @MockitoBean} 換成 Mockito mock 注入 Spring 容器。
 * 這樣就能單獨測「HTTP 介面層」:路由、JSON 序列化、狀態碼、錯誤轉換,
 * 而不被商業邏輯與外部依賴拖累。
 *
 * <p>({@code @MockitoBean} 是 Spring Boot 3.4+ 的寫法,
 * 取代已棄用的 {@code @MockBean}。)
 */
@WebMvcTest(OrderController.class)
class OrderControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;   // 整個 Service 層被 mock 掉

    @Test
    @DisplayName("POST /api/orders 成功 → 201,回傳訂單 JSON")
    void placeOrder_returns201() throws Exception {
        Order order = Order.create("SKU-KEYBOARD", 2, new BigDecimal("1500"));
        order.markPaid("TXN-WEB-1");
        when(orderService.placeOrder(any(PlaceOrderCommand.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": "SKU-KEYBOARD", "quantity": 2, "unitPrice": 1500}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(3000))
                .andExpect(jsonPath("$.transactionId").value("TXN-WEB-1"));
    }

    @Test
    @DisplayName("庫存不足 → 409 Conflict(驗證例外被 @RestControllerAdvice 正確轉換)")
    void outOfStock_returns409() throws Exception {
        when(orderService.placeOrder(any(PlaceOrderCommand.class)))
                .thenThrow(new OutOfStockException("SKU-LAPTOP"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": "SKU-LAPTOP", "quantity": 99, "unitPrice": 45000}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("商品庫存不足: SKU-LAPTOP"));
    }
}
