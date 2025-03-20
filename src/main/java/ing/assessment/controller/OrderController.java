package ing.assessment.controller;

import ing.assessment.db.order.Order;
import ing.assessment.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static ing.assessment.utils.GlobalConstants.Routes.ORDERS;
import static ing.assessment.utils.GlobalConstants.Routes.ORDER_BY_ID;

@RestController
@RequestMapping(ORDERS)
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping(ORDER_BY_ID)
    public Order getOrder(@PathVariable("id") Integer id) {
        return orderService.getOrderById(id);
    }
}
