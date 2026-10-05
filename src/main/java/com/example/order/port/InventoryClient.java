package com.example.order.port;

/**
 * 庫存服務(另一個微服務的抽象)。
 *
 * <p>測試時以 <b>Stub</b> 控制「有貨/沒貨」兩種情境,
 * 以 <b>Mock</b> 驗證「成功下單後有正確扣庫存」。
 */
public interface InventoryClient {

    /** 查詢庫存是否足夠。 */
    boolean isInStock(String productId, int quantity);

    /** 扣減庫存。 */
    void deduct(String productId, int quantity);
}
