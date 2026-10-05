package com.example.order.port;

import com.example.order.domain.Order;

/**
 * 通知服務(寄送訂單確認信)。
 *
 * <p>這是一個「只出不進」(outgoing, void)的依賴:
 * 它不回傳任何值,SUT 的回傳結果驗不出它有沒有被呼叫,
 * 所以測試時需要 <b>Spy</b>(記錄呼叫)或 <b>Mock</b>(驗證互動)。
 * 在與它無關的測試情境中,它則只是個 <b>Dummy</b>(填參數用)。
 */
public interface NotificationService {

    void sendOrderConfirmation(Order order);
}
