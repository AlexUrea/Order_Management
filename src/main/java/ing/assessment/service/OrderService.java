package ing.assessment.service;

import ing.assessment.db.order.Order;

import java.util.List;

public interface OrderService {
    Order createOrder(Order order);
    List<Order> getAllOrders();
    Order getOrderById(int id);
}