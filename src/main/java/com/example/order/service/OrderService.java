package com.example.order.service;

import com.example.order.domain.Order;
import com.example.order.exception.OutOfStockException;
import com.example.order.exception.PaymentFailedException;
import com.example.order.port.InventoryClient;
import com.example.order.port.NotificationService;
import com.example.order.port.OrderRepository;
import com.example.order.port.PaymentGateway;
import com.example.order.port.PaymentResult;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 下訂單的核心商業邏輯 —— 本教學的被測對象(SUT, System Under Test)。
 *
 * <p>流程:
 * <ol>
 *   <li>查庫存,不足 → 丟 {@link OutOfStockException}</li>
 *   <li>建立訂單並向金流扣款,失敗 → 丟 {@link PaymentFailedException}</li>
 *   <li>扣庫存、存訂單</li>
 *   <li>寄送訂單確認通知</li>
 * </ol>
 *
 * <p>四個協作者都以介面(port)注入,因此測試時可以逐一換成
 * Dummy / Stub / Spy / Mock / Fake。
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final InventoryClient inventoryClient;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
                        PaymentGateway paymentGateway,
                        InventoryClient inventoryClient,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.inventoryClient = inventoryClient;
        this.notificationService = notificationService;
    }

    public Order placeOrder(PlaceOrderCommand command) {
        if (!inventoryClient.isInStock(command.productId(), command.quantity())) {
            throw new OutOfStockException(command.productId());
        }

        Order order = Order.create(command.productId(), command.quantity(), command.unitPrice());

        PaymentResult result = paymentGateway.charge(order.getId(), order.getTotalAmount());
        if (!result.success()) {
            throw new PaymentFailedException(result.failureReason());
        }

        order.markPaid(result.transactionId());
        inventoryClient.deduct(command.productId(), command.quantity());
        Order saved = orderRepository.save(order);

        notificationService.sendOrderConfirmation(saved);
        return saved;
    }

    public Optional<Order> getOrder(String orderId) {
        return orderRepository.findById(orderId);
    }
}
