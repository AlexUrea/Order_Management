package ing.assessment.controller;

import ing.assessment.db.dto.ProductDTO;
import ing.assessment.db.product.Product;
import ing.assessment.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static ing.assessment.utils.GlobalConstants.Routes.PRODUCTS;
import static ing.assessment.utils.GlobalConstants.Routes.PRODUCTS_BY_ID;

@RestController
@RequestMapping(PRODUCTS)
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping(PRODUCTS_BY_ID)
    public List<ProductDTO> getProduct(@PathVariable("id") Integer id) {
        return productService.getProductsById(id);
    }
}