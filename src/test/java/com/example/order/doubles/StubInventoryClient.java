package com.example.order.doubles;

import com.example.order.port.InventoryClient;

/**
 * 【Stub】回傳「寫死答案」的庫存服務。
 *
 * <p>Stub 負責「餵資料給 SUT」(間接輸入, indirect input):
 * 測試開始前就決定好它要回答什麼,把 SUT 推進我們想測的分支。
 * 它不記錄、不驗證任何事。
 */
public class StubInventoryClient implements InventoryClient {

    private final boolean inStock;

    private StubInventoryClient(boolean inStock) {
        this.inStock = inStock;
    }

    /** 建立一個「永遠有貨」的 stub。 */
    public static StubInventoryClient alwaysInStock() {
        return new StubInventoryClient(true);
    }

    /** 建立一個「永遠缺貨」的 stub。 */
    public static StubInventoryClient alwaysOutOfStock() {
        return new StubInventoryClient(false);
    }

    @Override
    public boolean isInStock(String productId, int quantity) {
        return inStock;
    }

    @Override
    public void deduct(String productId, int quantity) {
        // Stub 對「輸出型」呼叫不做任何事,也不關心它有沒有發生
    }
}
