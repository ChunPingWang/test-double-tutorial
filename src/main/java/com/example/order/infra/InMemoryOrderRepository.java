package com.example.order.infra;

import com.example.order.domain.Order;
import com.example.order.port.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 正式程式用的記憶體版 Repository(教學專案以此代替真實資料庫)。
 * 真實專案中這裡會是 JPA / MyBatis / JDBC 實作。
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
