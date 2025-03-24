package ing.assessment.exceptions;

public final class ProductOutOfStock extends BusinessException {

    public ProductOutOfStock(String message) {
        super(message);
    }
}