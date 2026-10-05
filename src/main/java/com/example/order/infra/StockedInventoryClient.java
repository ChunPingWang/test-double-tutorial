package com.example.order.infra;

import com.example.order.port.InventoryClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模擬的庫存服務(教學專案以此代替真實的庫存微服務)。
 * 啟動時預載幾個商品的庫存。
 */
@Component
public class StockedInventoryClient implements InventoryClient {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of(
            "SKU-KEYBOARD", 50,
            "SKU-MONITOR", 10,
            "SKU-LAPTOP", 3
    ));

    @Override
    public boolean isInStock(String productId, int quantity) {
        return stock.getOrDefault(productId, 0) >= quantity;
    }

    @Override
    public void deduct(String productId, int quantity) {
        stock.computeIfPresent(productId, (id, current) -> current - quantity);
    }
}
