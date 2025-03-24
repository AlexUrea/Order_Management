package ing.assessment.exceptions;

/**
 * A sealed base class for all business exceptions.
 * Only the permitted subclasses may extend this exception.
 */
public sealed class BusinessException extends RuntimeException
        permits OrderNotFoundException, ProductOutOfStock, InsufficientStockException {

    public BusinessException(String message) {
        super(message);
    }
}
