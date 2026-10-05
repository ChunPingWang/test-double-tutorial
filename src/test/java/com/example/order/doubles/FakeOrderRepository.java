package com.example.order.doubles;

import com.example.order.domain.Order;
import com.example.order.port.OrderRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 【Fake】有「真功能」的輕量級 Repository。
 *
 * <p>Fake 是介面的完整可用實作,只是用了不適合上線的捷徑
 * (這裡用 HashMap 代替資料庫)。
 * 和 Stub 的差別:Stub 回寫死的答案,Fake 有真實一致的行為——
 * 存進去的訂單,真的查得回來。
 */
public class FakeOrderRepository implements OrderRepository {

    private final Map<String, Order> store = new HashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    /** 測試輔助:目前存了幾筆訂單。 */
    public int count() {
        return store.size();
    }
}
