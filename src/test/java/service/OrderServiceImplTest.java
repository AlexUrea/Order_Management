package service;

import ing.assessment.db.dto.OrderDTO;
import ing.assessment.db.dto.OrderProductDTO;
import ing.assessment.db.order.CostAndLocations;
import ing.assessment.db.order.Order;
import ing.assessment.db.order.OrderProduct;
import ing.assessment.db.product.Product;
import ing.assessment.db.product.ProductCK;
import ing.assessment.db.repository.OrderRepository;
import ing.assessment.exceptions.InsufficientStockException;
import ing.assessment.exceptions.OrderNotFoundException;
import ing.assessment.exceptions.ProductOutOfStock;
import ing.assessment.model.Location;
import ing.assessment.service.ProductService;
import ing.assessment.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ing.assessment.utils.GlobalConstants.Costs.DISCOUNT_10;
import static ing.assessment.utils.GlobalConstants.Costs.FREE_DELIVERY;
import static ing.assessment.utils.GlobalConstants.Exceptions.INSUFFICIENT_STOCK;
import static ing.assessment.utils.GlobalConstants.Exceptions.LOCATION_IS_NULL;
import static ing.assessment.utils.GlobalConstants.Exceptions.ORDER_ORDERPRODUCTS_NULL;
import static ing.assessment.utils.GlobalConstants.Exceptions.PRICE_IS_NULL;
import static ing.assessment.utils.GlobalConstants.Exceptions.PRODUCT_NOT_FOUND;
import static ing.assessment.utils.GlobalConstants.Exceptions.PRODUCT_OUT_OF_STOCK;
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
    @DisplayName("""
            getAllOrders when the repository returns an empty list.""")
    public void testGetAllOrdersEmpty() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        List<OrderDTO> result = orderService.getAllOrders();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("""
            getAllOrders when the repository returns some orders.""")
    public void testGetAllOrdersNonEmpty() {
        Order order1 = buildOrder();
        Order order2 = buildOrder();

        when(orderRepository.findAll()).thenReturn(Arrays.asList(order1, order2));

        List<OrderDTO> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(2, result.size());

        OrderDTO dto1 = result.get(0);
        assertEquals(order1.getId(), dto1.getId());
        assertEquals(order1.getTimestamp(), dto1.getTimestamp());
        assertEquals(order1.getOrderCost(), dto1.getOrderCost());
        assertEquals(order1.getDeliveryCost(), dto1.getDeliveryCost());
        assertEquals(order1.getDeliveryTime(), dto1.getDeliveryTime());

        OrderDTO dto2 = result.get(1);
        assertEquals(order2.getId(), dto2.getId());
        assertEquals(order2.getTimestamp(), dto2.getTimestamp());
        assertEquals(order2.getOrderCost(), dto2.getOrderCost());
        assertEquals(order2.getDeliveryCost(), dto2.getDeliveryCost());
        assertEquals(order2.getDeliveryTime(), dto2.getDeliveryTime());
    }

    @Test
    @DisplayName("""
            getOrderById when the repository returns an order.""")
    public void testGetOrderByIdFound() {
        Order order = buildOrder();
        int orderId = order.getId();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderDTO dto = orderService.getOrderById(orderId);

        assertNotNull(dto);
        assertEquals(order.getId(), dto.getId());
        assertEquals(order.getTimestamp(), dto.getTimestamp());
        assertEquals(order.getOrderCost(), dto.getOrderCost());
        assertEquals(order.getDeliveryCost(), dto.getDeliveryCost());
        assertEquals(order.getDeliveryTime(), dto.getDeliveryTime());
    }

    @Test
    @DisplayName("""
            getOrderById when the repository does NOT find an order""")
    public void testGetOrderByIdNotFound() {
        int orderId = 1;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(orderId);
        });

        String expectedMessage = String.format("Order with id %d not found", orderId);
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should be 'Order with id 1 not found'");
    }

    @Test
    @DisplayName("""
            calculateDiscountsAndDeliveryCost for orderCost <= ORDER_COST_500 ->
            no discount and deliveryCost remains unchanged.""")
    public void testCalculateDiscountsAndDeliveryCost_1() {
        Order order = buildOrder();
        double orderCost = 400.0;  // 400 <= ORDER_COST_500

        orderService.calculateDiscountsAndDeliveryCost(order, orderCost);

        assertEquals(orderCost, order.getOrderCost(), "Order cost should remain unchanged when <= 500.");
        assertEquals(30, order.getDeliveryCost(), "Delivery cost should remain unchanged when <= 500.");
    }

    @Test
    @DisplayName("""
            calculateDiscountsAndDeliveryCost for orderCost > 500 but <= 1000 ->
            free delivery applied, no discount.""")
    public void testCalculateDiscountsAndDeliveryCost_2() {
        Order order = buildOrder();
        double orderCost = 800.0; // 800 is > ORDER_COST_500 and <= ORDER_COST_1000

        orderService.calculateDiscountsAndDeliveryCost(order, orderCost);

        assertEquals(orderCost, order.getOrderCost(), "Order cost should remain unchanged when <= 1000");
        assertEquals(FREE_DELIVERY, order.getDeliveryCost(), "Delivery cost should be set to free delivery.");
    }

    @Test
    @DisplayName("""
            calculateDiscountsAndDeliveryCost for orderCost > 1000 ->
            10% discount applied and free delivery.""")
    public void testCalculateDiscountsAndDeliveryCost_3() {
        Order order = buildOrder();
        double orderCost = 1200.0;

        orderService.calculateDiscountsAndDeliveryCost(order, orderCost);

        assertEquals(orderCost * DISCOUNT_10, order.getOrderCost(), "Order cost should be discounted by 10%.");
        assertEquals(FREE_DELIVERY, order.getDeliveryCost(), "Delivery cost should be set to free delivery.");
    }

    @Test
    @DisplayName("""
            calculateDeliveryTime for locations == 1 ->
            No modification to deliveryTime (2 days).""")
    public void testCalculateDeliveryTime_1() {
        Order order = buildOrder();

        orderService.calculateDeliveryTime(order, 1);

        assertEquals(2, order.getDeliveryTime(), "Delivery time should remain unchanged for a single location.");
    }

    @Test
    @DisplayName("""
            calculateDeliveryTime for locations > 1 ->
            deliveryTime should be set to locations * 2.""")
    public void testCalculateDeliveryTime_2() {
        Order order = buildOrder();

        int locations = 3;
        orderService.calculateDeliveryTime(order, locations);

        assertEquals(6, order.getDeliveryTime(), "Delivery time should be 6 days (locations * 2).");
    }

    @Test
    @DisplayName("""
            adjustProductQuantities requestedQuantity is 0,
            no product quantities should be adjusted.
            """)
    public void testAdjustProductQuantities_1() {
        Product product = new Product();
        product.setQuantity(10);
        List<Product> products = new ArrayList<>();
        products.add(product);

        orderService.adjustProductQuantities(0, products);

        assertEquals(10, product.getQuantity(), "Quantity should remain unchanged when requestedQuantity is 0.");
    }

    @Test
    @DisplayName("""
            adjustProductQuantities first product's available quantity
            is greater than requestedQuantity.
            """)
    public void testAdjustProductQuantities_2() {
        Product product1 = new Product();
        product1.setQuantity(10);
        Product product2 = new Product();
        product2.setQuantity(5);

        List<Product> products = new ArrayList<>();
        products.add(product1);
        products.add(product2);

        orderService.adjustProductQuantities(5, products);

        assertEquals(5, product1.getQuantity(), "First product should be reduced by the requested quantity.");
        assertEquals(5, product2.getQuantity(), "Second product should remain unchanged.");
    }

    @Test
    @DisplayName("""
            adjustProductQuantities first product's available quantity
            is exactly equal to requestedQuantity.
            """)
    public void testAdjustProductQuantities_3() {
        Product product = new Product();
        product.setQuantity(5);
        List<Product> products = new ArrayList<>();
        products.add(product);

        orderService.adjustProductQuantities(5, products);

        assertEquals(0, product.getQuantity(), "Product quantity should become 0 when requested equals available.");
    }

    @Test
    @DisplayName("""
            adjustProductQuantities first product's available quantity
            is less than requestedQuantity, so adjustment continues to the next product.
            """)
    public void testAdjustProductQuantities_4() {
        Product product1 = new Product();
        product1.setQuantity(3);
        Product product2 = new Product();
        product2.setQuantity(10);

        List<Product> products = new ArrayList<>();
        products.add(product1);
        products.add(product2);

        orderService.adjustProductQuantities(5, products);

        assertEquals(0, product1.getQuantity(), "First product should be depleted.");
        assertEquals(8, product2.getQuantity(), "Second product should be reduced by the remaining quantity.");
    }

    // Scenario 1: All products have zero quantity – should throw ProductOutOfStock.
    @Test
    @DisplayName("""
            validateProductAvailability test productOutOfStock""")
    public void testValidateProductAvailability_1() {
        Product product = createProduct(1, 0, Location.MUNICH, 100.0);
        List<Product> products = Collections.singletonList(product);

        ProductOutOfStock exception = assertThrows(ProductOutOfStock.class, () ->
                orderService.validateProductAvailability(products, 5, 100.0)
        );

        String expectedMessage = String.format(PRODUCT_OUT_OF_STOCK, product.getProductCk().getId());
        assertEquals(expectedMessage, exception.getMessage(), "Product with id 1 out of stock in all locations");
    }

    @Test
    @DisplayName("""
            validateProductAvailability sufficient quantity in a single product""")
    public void testValidateProductAvailability_2() {
        Product product = createProduct(1, 5, Location.MUNICH, 150.0);
        List<Product> products = Collections.singletonList(product);
        int requestedQuantity = 3;
        double price = 150.0;

        CostAndLocations result = orderService.validateProductAvailability(products, requestedQuantity, price);
        double expectedCost = price * requestedQuantity;

        assertEquals(expectedCost, result.orderCost(), "orderCost expected to be 450");
        assertEquals(1, result.orderLocations(), "orderLocations expected to be 1");
    }

    @Test
    @DisplayName("""
            validateProductAvailability test null location""")
    public void testValidateProductAvailability_3() {
        Product product = createProduct(1, 5, null, 100.0);
        List<Product> products = Collections.singletonList(product);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.validateProductAvailability(products, 3, 100.0)
        );

        String expectedMessage = String.format(LOCATION_IS_NULL, product.getProductCk().getId());
        assertEquals(expectedMessage, exception.getMessage(), "Expected exception message 'Location of the product with id 1 must not be null'");
    }

    @Test
    @DisplayName("""
            validateProductAvailability test null price""")
    public void testValidateProductAvailability_4() {
        Product product = createProduct(1, 5, Location.COLOGNE, null);
        List<Product> products = Collections.singletonList(product);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.validateProductAvailability(products, 3, 100.0)
        );
        String expectedMessage = String.format(PRICE_IS_NULL, product.getProductCk().getId());
        assertEquals(expectedMessage, exception.getMessage(), "Expected exception message 'Price of the product with id 1 must not be null'");
    }

    @Test
    @DisplayName("""
            validateProductAvailability test InsufficientStockException""")
    public void testValidateProductAvailability_5() {
        Product product = createProduct(1, 2, Location.FRANKFURT, 100.0);
        List<Product> products = Collections.singletonList(product);
        int requestedQuantity = 5;

        InsufficientStockException exception = assertThrows(InsufficientStockException.class, () ->
                orderService.validateProductAvailability(products, requestedQuantity, 100.0)
        );
        String expectedMessage = String.format(INSUFFICIENT_STOCK, product.getProductCk().getId(), 2, requestedQuantity);
        assertEquals(expectedMessage, exception.getMessage(), "Expected exception message " + String.format(INSUFFICIENT_STOCK, product.getProductCk().getId(), 2, requestedQuantity));
    }

    @Test
    @DisplayName("""
            validateProductAvailability sufficient quantity
            across multiple products""")
    public void testValidateProductAvailability_6() {
        Product product1 = createProduct(1, 2, Location.MUNICH, 100.0);
        Product product2 = createProduct(2, 3, Location.COLOGNE, 100.0);
        List<Product> products = Arrays.asList(product1, product2);
        int requestedQuantity = 4;
        double price = 100.0;

        CostAndLocations result = orderService.validateProductAvailability(products, requestedQuantity, price);
        double expectedCost = price * requestedQuantity;

        assertEquals(expectedCost, result.orderCost(), "orderCost expected to be 400");
        assertEquals(2, result.orderLocations(), "orderLocations expected to be 2");
    }

    @Test
    @DisplayName("""
            processOrderProducts test null OrderProduct quantity""")
    public void testProcessOrderProducts_1() {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.setProductId(1);
        orderProduct.setQuantity(null);
        List<OrderProduct> orderProducts = Collections.singletonList(orderProduct);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.processOrderProducts(orderProducts)
        );
        String expectedMessage = String.format(QUANTITY_IS_NULL, orderProduct.getProductId());
        assertEquals(expectedMessage, exception.getMessage(), "Expected exception message 'Quantity of the orderProduct with productId 1 must not be null'");
    }

    @Test
    @DisplayName("""
            processOrderProducts test product not found""")
    public void testProcessOrderProducts_2() {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.setProductId(1);
        orderProduct.setQuantity(3);
        List<OrderProduct> orderProducts = Collections.singletonList(orderProduct);

        when(productService.getProductEntitiesById(1)).thenReturn(Collections.emptyList());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.processOrderProducts(orderProducts)
        );
        String expectedMessage = String.format(PRODUCT_NOT_FOUND, orderProduct.getProductId());
        assertEquals(expectedMessage, exception.getMessage(), "Expected exception message 'Product with id 1 not found'");
    }

    @Test
    @DisplayName("""
            processOrderProducts sufficient quantity in one product""")
    public void testProcessOrderProducts_3() {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.setProductId(1);
        orderProduct.setQuantity(3);
        List<OrderProduct> orderProducts = Collections.singletonList(orderProduct);

        Product product = createProduct(1, 10, Location.MUNICH, 100.0);
        List<Product> products = Collections.singletonList(product);
        when(productService.getProductEntitiesById(1)).thenReturn(products);

        CostAndLocations result = orderService.processOrderProducts(orderProducts);

        assertEquals(300.0, result.orderCost(), 0.001, "orderCost expected to be 300");
        assertEquals(1, result.orderLocations(), "orderLocations expected to be 1");
        assertEquals(7, product.getQuantity(), "Product quantity should be reduced by the requested quantity.");
    }

    @Test
    @DisplayName("""
            processOrderProducts test multiple OrderProducts with different products""")
    public void testProcessOrderProducts_4() {
        OrderProduct orderProduct1 = new OrderProduct();
        orderProduct1.setProductId(1);
        orderProduct1.setQuantity(3);

        OrderProduct orderProduct2 = new OrderProduct();
        orderProduct2.setProductId(2);
        orderProduct2.setQuantity(4);
        List<OrderProduct> orderProducts = Arrays.asList(orderProduct1, orderProduct2);

        Product product1 = createProduct(1, 5, Location.MUNICH, 100.0);
        List<Product> products1 = Collections.singletonList(product1);
        when(productService.getProductEntitiesById(1)).thenReturn(products1);

        Product product2a = createProduct(2, 1, Location.COLOGNE, 200.0);
        Product product2b = createProduct(2, 5, Location.FRANKFURT, 200.0);
        List<Product> products2 = Arrays.asList(product2a, product2b);
        when(productService.getProductEntitiesById(2)).thenReturn(products2);

        CostAndLocations result = orderService.processOrderProducts(orderProducts);

        assertEquals(1100.0, result.orderCost(), 0.001, "Expected orderCost to be 1100");
        assertEquals(2, result.orderLocations(), "orderLocations expected to be 2");

        assertEquals(2, product1.getQuantity(), "Product1 quantity expected to be 2 after adjustments");

        assertEquals(0, product2a.getQuantity(), "Product2a quantity expected to be 0 after adjustments");
        assertEquals(2, product2b.getQuantity(), "Product2b quantity expected to be 2 after adjustments");
    }

    @Test
    @DisplayName("""
            createOrder test null OrderDto""")
    public void testCreateOrder_1() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder(null)
        );
        assertEquals(ORDER_ORDERPRODUCTS_NULL, exception.getMessage(), "Expected exception message 'Order and OrderProducts cannot be null or empty'");
    }

    @Test
    @DisplayName("""
            createOrder test null OrderProducts""")
    public void testCreateOrder_2() {
        OrderDTO orderDto = new OrderDTO();
        orderDto.setOrderProducts(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder(orderDto)
        );
        assertEquals(ORDER_ORDERPRODUCTS_NULL, exception.getMessage(), "Expected exception message 'Order and OrderProducts cannot be null or empty'");
    }

    @Test
    @DisplayName("""
            createOrder test empty OrderProducts""")
    public void testCreateOrder_3() {
        OrderDTO orderDto = new OrderDTO();
        orderDto.setOrderProducts(Collections.emptyList());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder(orderDto)
        );
        assertEquals(ORDER_ORDERPRODUCTS_NULL, exception.getMessage(), "Expected exception message 'Order and OrderProducts cannot be null or empty'");
    }

    // Test a successful order creation.
    @Test
    @DisplayName("""
            createOrder test successful creation of order
            with stubbed 'save' persistence and costAndLocation """)
    public void testCreateOrder_4() {
        OrderDTO inputDto = new OrderDTO();
        OrderProductDTO opDto = new OrderProductDTO(1, 3);
        inputDto.setOrderProducts(Collections.singletonList(opDto));

        OrderServiceImpl spyService = spy(orderService);

        CostAndLocations fakeCostAndLocations = new CostAndLocations(600.0, 1);
        doReturn(fakeCostAndLocations).when(spyService).processOrderProducts(anyList());

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order orderArg = invocation.getArgument(0);
            orderArg.setId(1);
            return orderArg;
        });

        OrderDTO result = spyService.createOrder(inputDto);

        assertNotNull(result);
        assertEquals(1, result.getId(), "Order id should be 1");
        assertEquals(600.0, result.getOrderCost(), 0.001, "Order cost expected to be 600");
        assertEquals(FREE_DELIVERY, result.getDeliveryCost(), "Delivery cost expected to be 0");
        assertEquals(2, result.getDeliveryTime(), "Delivery time expected to be 2");
        assertNotNull(result.getTimestamp());

        verify(spyService).processOrderProducts(anyList());
        verify(orderRepository).save(any(Order.class));
    }


    private Order buildOrder() {
        Order order = new Order();
        order.setId(1);
        order.setTimestamp(new Date());
        order.setOrderCost(100.0);
        order.setOrderProducts(Collections.emptyList());

        return order;
    }

    private Product createProduct(int id, int quantity, Location location, Double price) {
        Product product = new Product();
        product.setQuantity(quantity);
        product.setPrice(price);
        product.setName("TestProduct" + id);
        ProductCK productCK = new ProductCK();
        productCK.setId(id);
        productCK.setLocation(location);
        product.setProductCk(productCK);
        return product;
    }
}

