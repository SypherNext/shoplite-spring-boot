package com.shoplite.common;

/**
 * Example error bodies shown in Swagger UI. They match what ApiExceptionHandler really returns.
 * Kept in one place so every controller shows the same examples.
 */
public final class ProblemExamples {

    public static final String NOT_FOUND = """
            {"title": "Not found", "status": 404, "detail": "Product 999 was not found", "instance": "/api/products/999"}
            """;

    public static final String VALIDATION_FAILED = """
            {"title": "Validation failed", "status": 400, "detail": "One or more fields are invalid",
             "instance": "/api/products", "errors": {"name": "must not be blank"}}
            """;

    public static final String CONFLICT = """
            {"title": "Conflict", "status": 409, "detail": "A product with SKU MUG-ENAMEL-01 already exists",
             "instance": "/api/products"}
            """;

    public static final String CATEGORY_IN_USE = """
            {"title": "Conflict", "status": 409, "detail": "Category 1 still has products. Move or delete them first",
             "instance": "/api/categories/1"}
            """;

    public static final String STALE_VERSION = """
            {"title": "Conflict", "status": 409,
             "detail": "The data was changed by another request. Reload it and try again",
             "instance": "/api/admin/orders/1/status"}
            """;

    public static final String KEY_REUSED = """
            {"title": "Unprocessable request", "status": 422,
             "detail": "Idempotency-Key order-1001 was already used with a different request body",
             "instance": "/api/orders"}
            """;

    public static final String PAYMENT_UNAVAILABLE = """
            {"title": "Service unavailable", "status": 503,
             "detail": "The payment provider is not responding. Retry with the same Idempotency-Key",
             "instance": "/api/orders"}
            """;

    private ProblemExamples() {
    }

}
