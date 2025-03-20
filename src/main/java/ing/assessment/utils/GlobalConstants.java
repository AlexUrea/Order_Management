package ing.assessment.utils;

public class GlobalConstants {
    public static final class Routes {
        public static final String ORDERS = "/orders";
        public static final String PRODUCTS = "/products";
        public static final String ORDER_BY_ID = "/{id}";
        public static final String PRODUCTS_BY_ID = "/{id}";
    }

    public static final class Costs {
        public static final int FREE_DELIVERY = 0;
        public static final double DISCOUNT_10 = 0.9;
        public static final int ORDER_COST_500 = 500;
        public static final int ORDER_COST_1000 = 1000;
    }

    public static final class Exceptions {
        public static final String LOCATION_IS_NULL = "Location of the product must not be null";
        public static final String PRICE_IS_NULL = "Price of the product must not be null";
        public static final String QUANTITY_IS_NULL = "Quantity of the product must not be null";
        public static final String INSUFFICIENT_STOCK = """
                                           Insufficient stock for product with id %s.
                                           Available: %s, requested: %s.
                                           """;
    }
}
