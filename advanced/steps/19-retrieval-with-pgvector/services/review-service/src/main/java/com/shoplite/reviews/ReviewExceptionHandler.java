package com.shoplite.reviews;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The first demo of this service answered a second review from the same customer with a bare 500:
 * the unique index (one review per customer and product) said no, and nothing translated it.
 * Errors from WebFlux itself (404, 400) become Problem Details through
 * spring.webflux.problemdetails.enabled=true.
 */
@RestControllerAdvice
class ReviewExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail alreadyReviewed(DataIntegrityViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "You have already reviewed this product");
        problem.setTitle("Conflict");
        return problem;
    }

}