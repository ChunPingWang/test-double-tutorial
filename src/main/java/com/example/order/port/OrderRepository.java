package com.example.order.port;

import com.example.order.domain.Order;

import java.util.Optional;

/**
 * 訂單儲存庫(資料存取的抽象)。
 *
 * <p>測試時通常以 <b>Fake</b>(記憶體版實作)取代,
 * 因為 Repository 的「查得回來」語意很重要,用 Stub 寫死回傳值反而容易失真。
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(String id);
}
