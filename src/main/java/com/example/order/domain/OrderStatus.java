package com.example.order.domain;

/**
 * 訂單狀態。
 */
public enum OrderStatus {
    /** 已建立,尚未付款 */
    PENDING,
    /** 付款成功,訂單成立 */
    CONFIRMED
}
