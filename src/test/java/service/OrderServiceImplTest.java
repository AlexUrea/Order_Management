package service;

import ing.assessment.db.order.Order;
import ing.assessment.db.order.OrderProduct;
import ing.assessment.db.product.Product;
import ing.assessment.db.product.ProductCK;
import ing.assessment.db.repository.OrderRepository;
import ing.assessment.exceptions.OrderNotFoundException;
import ing.assessment.model.Location;
import ing.assessment.service.ProductService;
import ing.assessment.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ing.assessment.utils.GlobalConstants.Costs.DISCOUNT_10;
import static ing.assessment.utils.GlobalConstants.Costs.FREE_DELIVERY;
import static ing.assessment.utils.GlobalConstants.Exceptions.INSUFFICIENT_STOCK;
import static ing.assessment.utils.GlobalConstants.Exceptions.LOCATION_IS_NULL;
import static ing.assessment.utils.GlobalConstants.Exceptions.PRICE_IS_NULL;
import static ing.assessment.utils.GlobalConstants.Exceptions.QUANTITY_IS_NULL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    public void testGetAllOrders_returnsList() {
        Order order1 = new Order();
        order1.setId(1);
        Order order2 = new Order();
        order2.setId(2);
        List<Order> orders = Arrays.asList(order1, order2);
        when(orderRepository.findAll()).thenReturn(orders);

        List<Order> result = orderService.getAllOrders();

        assertEquals(orders, result);
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    public void testGetAllOrders_emptyList() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        List<Order> result = orderService.getAllOrders();

        assertTrue(result.isEmpty());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    public void testGetOrderById_orderFound() {
        Order order = new Order();
        order.setId(1);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(1);

        assertEquals(order, result);
        verify(orderRepository, times(1)).findById(1);
    }

    @Test
    public void testGetOrderById_orderNotFound_throwsException() {
        int id = 42;
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrderById(id));

        String expectedMessage = "Order with id " + id + " not found";
        assertEquals(expectedMessage, exception.getMessage());
        verify(orderRepository, times(1)).findById(id);
    }

    @Test
    public void testCalculateDiscountsAndDeliveryCost_OrderCostLessThanOrEqualTo500() {
        Order order = new Order();
        order.setOrderCost(400.0);

        double inputOrderCost = 400.0;
        orderService.calculateDiscountsAndDeliveryCost(order, inputOrderCost);

        assertEquals(400.0, order.getOrderCost());
        assertEquals(30, order.getDeliveryCost());
    }

    @Test
    public void testCalculateDiscountsAndDeliveryCost_OrderCostGreaterThan1000() {
        Order order = new Order();
        double inputOrderCost = 1500.0;
        orderService.calculateDiscountsAndDeliveryCost(order, inputOrderCost);

        double expectedDiscountedCost = inputOrderCost * DISCOUNT_10;
        assertEquals(expectedDiscountedCost, order.getOrderCost());
        assertEquals(FREE_DELIVERY, order.getDeliveryCost());
    }

    @Test
    public void testCalculateDiscountsAndDeliveryCost_OrderCostBetween500And1000() {
        Order order = new Order();
        order.setOrderCost(600.0);

        double inputOrderCost = 600.0;
        orderService.calculateDiscountsAndDeliveryCost(order, inputOrderCost);

        assertEquals(600.0, order.getOrderCost());
        assertEquals(FREE_DELIVERY, order.getDeliveryCost());
    }

    @Test
    public void testCalculateDeliveryTime_SingleLocation() {
        Order order = new Order();
        Set<Location> locations = new HashSet<>();
        locations.add(Location.MUNICH);

        orderService.calculateDeliveryTime(order, locations);

        assertEquals(2, order.getDeliveryTime());
    }

    @Test
    public void testCalculateDeliveryTime_MultipleLocations() {
        Order order = new Order();
        Set<Location> locations = new HashSet<>();
        locations.add(Location.MUNICH);
        locations.add(Location.COLOGNE);

        orderService.calculateDeliveryTime(order, locations);

        assertEquals(4, order.getDeliveryTime());
    }

    @Test
    public void testValidateProductAvailability_valid() {
        ProductCK productCK = new ProductCK(1, Location.MUNICH);
        Product product = new Product(productCK, "Test Product", 100.0, 10);
        int requestedQuantity = 5;

        assertDoesNotThrow(() -> orderService.validateProductAvailability(product, requestedQuantity));
    }

    @Test
    public void testValidateProductAvailability_quantityIsNull() {
        ProductCK productCK = new ProductCK(1, Location.MUNICH);
        Product product = new Product(productCK, "Test Product", 100.0, null);
        int requestedQuantity = 5;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.validateProductAvailability(product, requestedQuantity));
        assertEquals(QUANTITY_IS_NULL, ex.getMessage());
    }

    @Test
    public void testValidateProductAvailability_insufficientStock() {
        int requestedQuantity = 5;
        ProductCK productCK = new ProductCK(1, Location.MUNICH);
        Product product = new Product(productCK, "Test Product", 100.0, 3);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.validateProductAvailability(product, requestedQuantity));
        String expectedMessage = String.format(INSUFFICIENT_STOCK, productCK.getId(), product.getQuantity(), requestedQuantity);
        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    public void testValidateProductAvailability_priceIsNull() {
        ProductCK productCK = new ProductCK(1, Location.MUNICH);
        Product product = new Product(productCK, "Test Product", null, 10);
        int requestedQuantity = 5;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.validateProductAvailability(product, requestedQuantity));
        assertEquals(PRICE_IS_NULL, ex.getMessage());
    }

    @Test
    public void testValidateProductAvailability_locationIsNull() {
        ProductCK productCK = new ProductCK(1, null);
        Product product = new Product(productCK, "Test Product", 100.0, 10);
        int requestedQuantity = 5;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.validateProductAvailability(product, requestedQuantity));
        assertEquals(LOCATION_IS_NULL, ex.getMessage());
    }

    @Test
    public void testProcessOrderProducts_validSingleOrderProduct() {
        ProductCK productCK = new ProductCK(1, Location.MUNICH);
        Product product = new Product(productCK, "Test Product", 100.0, 10);
        OrderProduct op = new OrderProduct(1, 2);
        List<OrderProduct> orderProducts = Collections.singletonList(op);
        Set<Location> locations = new HashSet<>();

        when(productService.getProductsById(1)).thenReturn(Collections.singletonList(product));

        double orderCost = orderService.processOrderProducts(orderProducts, locations);

        assertEquals(200.0, orderCost);
        assertEquals(8, product.getQuantity());
        assertTrue(locations.contains(Location.MUNICH));
    }

    @Test
    public void testProcessOrderProducts_validMultipleOrderProducts() {
        ProductCK productCK1 = new ProductCK(1, Location.MUNICH);
        Product product1 = new Product(productCK1, "Test Product 1", 50.0, 10);
        ProductCK productCK2 = new ProductCK(2, Location.COLOGNE);
        Product product2 = new Product(productCK2, "Test Product 2", 100.0, 20);

        OrderProduct op1 = new OrderProduct(1, 3);
        OrderProduct op2 = new OrderProduct(2, 5);
        List<OrderProduct> orderProducts = Arrays.asList(op1, op2);
        Set<Location> locations = new HashSet<>();

        when(productService.getProductsById(1)).thenReturn(Collections.singletonList(product1));
        when(productService.getProductsById(2)).thenReturn(Collections.singletonList(product2));

        double orderCost = orderService.processOrderProducts(orderProducts, locations);

        assertEquals(650.0, orderCost);
        assertEquals(10 - 3, product1.getQuantity());
        assertEquals(20 - 5, product2.getQuantity());
        assertTrue(locations.contains(Location.MUNICH));
        assertTrue(locations.contains(Location.COLOGNE));
    }

    @Test
    public void testProcessOrderProducts_productNotFound() {
        OrderProduct op = new OrderProduct(1, 1);
        List<OrderProduct> orderProducts = Collections.singletonList(op);
        Set<Location> locations = new HashSet<>();

        when(productService.getProductsById(1)).thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.processOrderProducts(orderProducts, locations));
        String expectedMessage = "Product with id " + op.getProductId() + " not found.";
        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    public void testProcessOrderProducts_insufficientStock() {
        ProductCK productCK = new ProductCK(1, Location.MUNICH);
        Product product = new Product(productCK, "Test Product", 100.0, 1);
        OrderProduct op = new OrderProduct(1, 2);
        List<OrderProduct> orderProducts = Collections.singletonList(op);
        Set<Location> locations = new HashSet<>();

        when(productService.getProductsById(1)).thenReturn(Collections.singletonList(product));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.processOrderProducts(orderProducts, locations));
        String expectedMessage = String.format(INSUFFICIENT_STOCK, productCK.getId(), product.getQuantity(), op.getQuantity());
        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    public void testProcessOrderProducts_emptyList() {
        List<OrderProduct> orderProducts = Collections.emptyList();
        Set<Location> locations = new HashSet<>();

        double orderCost = orderService.processOrderProducts(orderProducts, locations);

        assertEquals(0.0, orderCost);
        assertTrue(locations.isEmpty());
    }

    @Test
    public void testCreateOrder_NullOrder() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(null));
        assertEquals("Order and OrderProducts cannot be null or empty", ex.getMessage());
    }

    @Test
    public void testCreateOrder_NullOrderProducts() {
        Order order = new Order();
        order.setOrderProducts(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(order));
        assertEquals("Order and OrderProducts cannot be null or empty", ex.getMessage());
    }

    @Test
    public void testCreateOrder_EmptyOrderProducts() {
        Order order = new Order();
        order.setOrderProducts(Collections.emptyList());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(order));
        assertEquals("Order and OrderProducts cannot be null or empty", ex.getMessage());
    }

    @Test
    public void testCreateOrder_OrderCostLessThanOrEqualTo500() {
        ProductCK productCK = new ProductCK(1, Location.MUNICH);
        Product product = new Product(productCK, "Test Product", 200.0, 5);
        OrderProduct op = new OrderProduct(1, 1);
        List<OrderProduct> orderProducts = Collections.singletonList(op);

        Order order = new Order();
        order.setOrderProducts(orderProducts);

        when(productService.getProductsById(1)).thenReturn(Collections.singletonList(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order createdOrder = orderService.createOrder(order);

        assertEquals(30, createdOrder.getDeliveryCost());
        assertEquals(4, product.getQuantity());
        assertEquals(2, createdOrder.getDeliveryTime());
        assertNotNull(createdOrder.getTimestamp());
    }

    @Test
    public void testCreateOrder_OrderCostBetween500And1000() {
        ProductCK productCK = new ProductCK(1, Location.MUNICH);
        Product product = new Product(productCK, "Test Product", 300.0, 5);
        OrderProduct op = new OrderProduct(1, 2);
        List<OrderProduct> orderProducts = Collections.singletonList(op);

        Order order = new Order();
        order.setOrderProducts(orderProducts);

        when(productService.getProductsById(1)).thenReturn(Collections.singletonList(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order createdOrder = orderService.createOrder(order);

        assertEquals(FREE_DELIVERY, createdOrder.getDeliveryCost());
        assertNull(createdOrder.getOrderCost());
        assertEquals(3, product.getQuantity());
        assertEquals(2, createdOrder.getDeliveryTime());
        assertNotNull(createdOrder.getTimestamp());
    }

    @Test
    public void testCreateOrder_OrderCostGreaterThan1000() {
        ProductCK productCK = new ProductCK(1, Location.MUNICH);
        Product product = new Product(productCK, "Test Product", 600.0, 5);
        OrderProduct op = new OrderProduct(1, 2);
        List<OrderProduct> orderProducts = Collections.singletonList(op);

        Order order = new Order();
        order.setOrderProducts(orderProducts);

        when(productService.getProductsById(1)).thenReturn(Collections.singletonList(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order createdOrder = orderService.createOrder(order);

        double expectedDiscountedCost = 1200 * DISCOUNT_10;
        assertEquals(expectedDiscountedCost, createdOrder.getOrderCost());
        assertEquals(FREE_DELIVERY, createdOrder.getDeliveryCost());
        assertEquals(3, product.getQuantity());
        assertEquals(2, createdOrder.getDeliveryTime());
        assertNotNull(createdOrder.getTimestamp());
    }

    @Test
    public void testCreateOrder_MultipleUniqueLocations() {
        ProductCK productCK1 = new ProductCK(1, Location.MUNICH);
        Product product1 = new Product(productCK1, "Product 1", 200.0, 5);
        OrderProduct op1 = new OrderProduct(1, 1);

        ProductCK productCK2 = new ProductCK(2, Location.COLOGNE);
        Product product2 = new Product(productCK2, "Product 2", 300.0, 5);
        OrderProduct op2 = new OrderProduct(2, 1);

        List<OrderProduct> orderProducts = Arrays.asList(op1, op2);

        Order order = new Order();
        order.setOrderProducts(orderProducts);

        when(productService.getProductsById(1)).thenReturn(Collections.singletonList(product1));
        when(productService.getProductsById(2)).thenReturn(Collections.singletonList(product2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order createdOrder = orderService.createOrder(order);

        assertEquals(30, createdOrder.getDeliveryCost());
        assertEquals(4, createdOrder.getDeliveryTime());
        assertEquals(4, product1.getQuantity());
        assertEquals(4, product2.getQuantity());
        assertNotNull(createdOrder.getTimestamp());
    }
}

