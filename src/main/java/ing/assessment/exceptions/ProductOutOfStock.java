package ing.assessment.exceptions;

public class ProductOutOfStock extends RuntimeException {
    public ProductOutOfStock(String message) {
        super(message);
    }
}