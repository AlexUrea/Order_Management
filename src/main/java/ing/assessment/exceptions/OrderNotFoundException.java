package ing.assessment.exceptions;

public final class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}