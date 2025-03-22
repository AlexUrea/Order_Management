package ing.assessment.service;

import ing.assessment.db.dto.ProductDTO;
import ing.assessment.db.product.Product;
import java.util.List;

public interface ProductService {
    List<ProductDTO> getAllProducts();
    List<ProductDTO> getProductsById(Integer id);
    List<Product> getProductEntitiesById(Integer id);
}