package ing.assessment.service.impl;

import ing.assessment.db.order.CostAndLocations;
import ing.assessment.db.order.Order;
import ing.assessment.db.order.OrderProduct;
import ing.assessment.db.product.Product;
import ing.assessment.db.repository.OrderRepository;
import ing.assessment.exceptions.InsufficientStockException;
import ing.assessment.exceptions.OrderNotFoundException;
import ing.assessment.exceptions.ProductOutOfStock;
import ing.assessment.model.Location;
import ing.assessment.service.OrderService;
import ing.assessment.service.ProductService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
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
import static ing.assessment.utils.GlobalConstants.Exceptions.ORDER_NOT_FOUND;
import static ing.assessment.utils.GlobalConstants.Exceptions.ORDER_ORDERPRODUCTS_NULL;
import static ing.assessment.utils.GlobalConstants.Exceptions.PRICE_IS_NULL;
import static ing.assessment.utils.GlobalConstants.Exceptions.PRODUCT_NOT_FOUND;
import static ing.assessment.utils.GlobalConstants.Exceptions.PRODUCT_OUT_OF_STOCK;
import static ing.assessment.utils.GlobalConstants.Exceptions.QUANTITY_IS_NULL;

@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger LOG = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public OrderServiceImpl(OrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @Override
    public List<Order> getAllOrders() {
        LOG.info("Returning all orders");
        return orderRepository.findAll();
    }

    @Override
    public Order getOrderById(int id) {
        LOG.info("Returning order with id {}", id);
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(String.format(ORDER_NOT_FOUND, id)));
    }

    @Override
    @Transactional
    public Order createOrder(Order order) {
        if (order == null || order.getOrderProducts() == null || order.getOrderProducts().isEmpty()) {
            throw new IllegalArgumentException(ORDER_ORDERPRODUCTS_NULL);
        }
        List<OrderProduct> orderProducts = order.getOrderProducts();
        LOG.info("Creating order with id {} and products {}", order.getId(), orderProducts);

        CostAndLocations orderCostAndLocations = processOrderProducts(orderProducts);
        LOG.info("orderCostAndLocations: {} after processOrderProducts", orderCostAndLocations);

        // Apply cost rules: free delivery for orders > 500, plus discount for orders > 1000
        calculateDiscountsAndDeliveryCost(order, orderCostAndLocations.orderCost());

        // Compute delivery time: base is 2 days; add 2 extra days per additional unique location
        calculateDeliveryTime(order, orderCostAndLocations.orderLocations());

        //timestamp set in UTC to avoid confusion. Consumers are free to format the date as they need
        order.setTimestamp(Date.from(Instant.now()));

        LOG.info("Order with id {} created at {}", order.getId(), LocalDateTime.now());        return orderRepository.save(order);
    }

    /**
     * Processes the order products by validating stock and location availability, decreasing product quantities,
     * calculating the total order cost, and collecting product locations.
     */
    public CostAndLocations processOrderProducts(List<OrderProduct> orderProducts) {
        int totalLocations = 0;
        double orderCost = 0;

        for (OrderProduct orderProduct : orderProducts) {
            if (orderProduct.getQuantity() == null) {
                throw new IllegalArgumentException(String.format(QUANTITY_IS_NULL, orderProduct.getProductId()));
            }
            Integer requestedQuantity = orderProduct.getQuantity();
            List<Product> products = productService.getProductsById(orderProduct.getProductId());
            if (products.isEmpty()) {
                throw new IllegalArgumentException(String.format(PRODUCT_NOT_FOUND, orderProduct.getProductId()));
            }

            CostAndLocations productCostAndLocations = validateProductAvailability(products, requestedQuantity, products.get(0).getPrice());
            int currentProductLocations = productCostAndLocations.orderLocations();
            if (totalLocations < currentProductLocations) {
                totalLocations = currentProductLocations;
            }

            orderCost += productCostAndLocations.orderCost();

            adjustProductQuantities(requestedQuantity, products);
        }

        return new CostAndLocations(orderCost, totalLocations);
    }

    /**
     * Validates that the product has enough stock for the requested quantity,
     * as well as a valid location and price
     */
    public CostAndLocations validateProductAvailability(List<Product> products, Integer requestedQuantity, Double price) {
        Set<Location> locations = new HashSet<>();

        int productQuantity = 0;
        for (Product product : products) {
            if (product.getQuantity() == 0) {
                continue;
            }
            productQuantity += product.getQuantity();

            if (product.getProductCk().getLocation() == null) {
                throw new IllegalArgumentException(String.format(LOCATION_IS_NULL, product.getProductCk().getId()));
            }
            locations.add(product.getProductCk().getLocation());

            if (productQuantity >= requestedQuantity) {
                if (product.getPrice() == null) {
                    throw new IllegalArgumentException(String.format(PRICE_IS_NULL, product.getProductCk().getId()));
                }
                Double orderCost = price * requestedQuantity;
                return new CostAndLocations(orderCost, locations.size());
            }
        }

        if (productQuantity == 0) {
            throw new ProductOutOfStock(String.format(PRODUCT_OUT_OF_STOCK, products.get(0).getProductCk().getId()));
        }
        throw new InsufficientStockException(String.format(
                INSUFFICIENT_STOCK,
                products.get(0).getProductCk().getId(),
                productQuantity,
                requestedQuantity));
    }

    public void adjustProductQuantities(Integer requestedQuantity, List<Product> products) {
        int remainingQuantity = requestedQuantity;

        for (Product product : products) {
            if (remainingQuantity <= 0) {
                break;
            }

            int availableQuantity = product.getQuantity();
            if (availableQuantity >= remainingQuantity) {
                product.setQuantity(availableQuantity - remainingQuantity);
                remainingQuantity = 0;
            } else {
                product.setQuantity(0);
                remainingQuantity -= availableQuantity;
            }
        }
    }

    public void calculateDiscountsAndDeliveryCost(Order order, double orderCost) {
        if (orderCost <= ORDER_COST_500) {
            LOG.info("For order with id {} and orderCost {} no discount", order.getId(), orderCost);
            order.setOrderCost(orderCost);
            return;
        }
        if (orderCost > ORDER_COST_1000) {
            LOG.info("For order with id {} and orderCost {} free delivery and 10% discount", order.getId(), orderCost);
            order.setOrderCost(orderCost * DISCOUNT_10); // 10% discount
            order.setDeliveryCost(FREE_DELIVERY);
            return;
        }
        LOG.info("For order with id {} and orderCost {} free delivery and no discount", order.getId(), orderCost);
        order.setOrderCost(orderCost);
        order.setDeliveryCost(FREE_DELIVERY);
    }

    public void calculateDeliveryTime(Order order, int locations) {
        if (locations == 1) {
            LOG.info("For order with id {} delivery time is {}", order.getId(), order.getDeliveryTime());
            return;
        }
        int deliveryTime = locations * 2;
        LOG.info("For order with id {} delivery time is {}", order.getId(), deliveryTime);
        order.setDeliveryTime(deliveryTime);
    }
}