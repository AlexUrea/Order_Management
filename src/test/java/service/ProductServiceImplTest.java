package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import ing.assessment.db.product.Product;
import ing.assessment.db.product.ProductCK;
import ing.assessment.db.repository.ProductRepository;
import ing.assessment.model.Location;
import ing.assessment.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    public void testGetAllProducts() {
        ProductCK productCK1 = new ProductCK(1, Location.MUNICH);
        Product product1 = new Product(productCK1, "Test Product 1", 10.0, 100);

        ProductCK productCK2 = new ProductCK(2, Location.COLOGNE);
        Product product2 = new Product(productCK2, "Test Product 2", 15.0, 200);

        List<Product> productList = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(productList);

        List<Product> result = productService.getAllProducts();

        assertEquals(productList, result);
        verify(productRepository, times(1)).findAll();
    }

    @Test
    public void testGetProductsById() {
        Integer productId = 1;
        ProductCK productCK = new ProductCK(productId, Location.MUNICH);
        Product product = new Product(productCK, "Test Product", 20.0, 150);
        List<Product> productList = List.of(product);
        when(productRepository.findByProductCk_Id(productId)).thenReturn(productList);

        List<Product> result = productService.getProductsById(productId);

        assertEquals(productList, result);
        verify(productRepository, times(1)).findByProductCk_Id(productId);
    }
}

