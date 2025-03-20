package ing.assessment.service.impl;

import ing.assessment.db.order.Order;
import ing.assessment.db.order.OrderProduct;
import ing.assessment.db.product.Product;
import ing.assessment.db.repository.OrderRepository;
import ing.assessment.exceptions.OrderNotFoundException;
import ing.assessment.model.Location;
import ing.assessment.service.OrderService;
import ing.assessment.service.ProductService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ing.assessment.utils.GlobalConstants.Costs.DISCOUNT_10;
import static ing.assessment.utils.GlobalConstants.Costs.FREE_DELIVERY;
import static ing.assessment.utils.GlobalConstants.Costs.ORDER_COST_1000;
import static ing.assessment.utils.GlobalConstants.Costs.ORDER_COST_500;
import static ing.assessment.utils.GlobalConstants.Exceptions.INSUFFICIENT_STOCK;
import static ing.assessment.utils.GlobalConstants.Exceptions.LOCATION_IS_NULL;
import static ing.assessment.utils.GlobalConstants.Exceptions.PRICE_IS_NULL;
import static ing.assessment.utils.GlobalConstants.Exceptions.QUANTITY_IS_NULL;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public OrderServiceImpl(OrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order getOrderById(int id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException("Order with id " + id + " not found"));
    }

    @Override
    @Transactional
    public Order createOrder(Order order) {
        if (order == null || order.getOrderProducts() == null || order.getOrderProducts().isEmpty()) {
            throw new IllegalArgumentException("Order and OrderProducts cannot be null or empty");
        }
        double orderCost;
        Set<Location> locations = new HashSet<>();

        List<OrderProduct> orderProducts = order.getOrderProducts();

        orderCost = processOrderProducts(orderProducts, locations);

        // Apply cost rules: free delivery for orders > 500, plus discount for orders > 1000
        calculateDiscountsAndDeliveryCost(order, orderCost);

        // Compute delivery time: base is 2 days; add 2 extra days per additional unique location
        calculateDeliveryTime(order, locations);

        order.setTimestamp(new Date());

        return orderRepository.save(order);
    }

    /**
     * Processes the order products by validating stock and location availability, decreasing product quantities,
     * calculating the total order cost, and collecting product locations.
     *
     * @param orderProducts the list of products in the order.
     * @param locations     a set to collect the unique locations of the products.
     * @return the total order cost.
     */
    public double processOrderProducts(List<OrderProduct> orderProducts, Set<Location> locations) {
        double orderCost = 0.0;
        for (OrderProduct op : orderProducts) {
            List<Product> products = productService.getProductsById(op.getProductId());
            if (products.isEmpty()) {
                throw new IllegalArgumentException("Product with id " + op.getProductId() + " not found.");
            }
            Product product = products.get(0);

            validateProductAvailability(product, op.getQuantity());

            product.setQuantity(product.getQuantity() - op.getQuantity());

            orderCost += product.getPrice() * op.getQuantity();
            locations.add(product.getProductCk().getLocation());
        }
        return orderCost;
    }

    /**
     * Validates that the product has enough stock for the requested quantity,
     * as well as a valid location and price
     *
     * @param product           the product to validate.
     * @param requestedQuantity the requested quantity.
     */
    public void validateProductAvailability(Product product, int requestedQuantity) {
        if (product.getQuantity() == null) {
            throw new IllegalArgumentException(QUANTITY_IS_NULL);
        }

        if (product.getQuantity() < requestedQuantity) {
            throw new IllegalArgumentException(
                    String.format(INSUFFICIENT_STOCK, product.getProductCk().getId(), product.getQuantity(), requestedQuantity)
            );
        }

        if (product.getPrice() == null) {
            throw new IllegalArgumentException(PRICE_IS_NULL);
        }

        if (product.getProductCk().getLocation() == null) {
            throw new IllegalArgumentException(LOCATION_IS_NULL);
        }
    }

    public void calculateDiscountsAndDeliveryCost(Order order, double orderCost) {
        if (orderCost <= ORDER_COST_500) {
            return;
        }
        if (orderCost > ORDER_COST_1000) {
            order.setOrderCost(orderCost * DISCOUNT_10); // 10% discount
            order.setDeliveryCost(FREE_DELIVERY);
            return;
        }
        order.setDeliveryCost(FREE_DELIVERY);
    }

    public void calculateDeliveryTime(Order order, Set<Location> locations) {
        if (locations.size() == 1) {
            return;
        }
        order.setDeliveryTime(locations.size() * 2);
    }
}