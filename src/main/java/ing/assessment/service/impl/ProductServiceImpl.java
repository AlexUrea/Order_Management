package ing.assessment.service.impl;

import ing.assessment.db.product.Product;
import ing.assessment.db.repository.ProductRepository;
import ing.assessment.db.dto.ProductDTO;
import ing.assessment.service.ProductService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<ProductDTO> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getProductsById(Integer id) {
        List<Product> products = productRepository.findByProductCk_Id(id);
        return products.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<Product> getProductEntitiesById(Integer id) {
        return productRepository.findByProductCk_Id(id);
    }

    private ProductDTO convertToDto(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getProductCk().getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setQuantity(product.getQuantity());
        dto.setLocation(product.getProductCk().getLocation());
        return dto;
    }
}