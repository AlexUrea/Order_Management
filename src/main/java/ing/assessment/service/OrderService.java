package ing.assessment.service;

import ing.assessment.db.dto.OrderDTO;
import ing.assessment.db.order.Order;

import java.util.List;

public interface OrderService {
    OrderDTO createOrder(OrderDTO orderDto);
    List<OrderDTO> getAllOrders();
    OrderDTO getOrderById(int id);
}