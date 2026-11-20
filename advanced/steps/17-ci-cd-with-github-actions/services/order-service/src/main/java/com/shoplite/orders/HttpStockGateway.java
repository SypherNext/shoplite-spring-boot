package com.shoplite.orders;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.shoplite.common.ConflictException;
import com.shoplite.common.NotFoundException;

/** The stock API over HTTP and JSON (lesson 7), still available with shoplite.catalog.transport=http. */
@Component
@ConditionalOnProperty(name = "shoplite.catalog.transport", havingValue = "http")
class HttpStockGateway implements StockGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpStockGateway.class);

    private final CatalogStockClient client;

    HttpStockGateway(CatalogStockClient client) {
        this.client = client;
    }

    @Override
    public List<ReservedProduct> reserve(UUID reservationId, List<OrderLineRequest> lines) {
        try {
            return client.reserve(new CatalogStockClient.Reservation(reservationId, lines));
        } catch (HttpClientErrorException ex) {
            throw translate(ex);
        } catch (RestClientException ex) {
            log.warn("Catalog reservation {} failed: {}", reservationId, ex.getMessage());
            throw new CatalogUnavailableException(ex);
        }
    }

    @Override
    public void release(UUID reservationId) {
        try {
            client.release(reservationId);
        } catch (RestClientException ex) {
            throw new CatalogUnavailableException(ex);
        }
    }

    private static RuntimeException translate(HttpClientErrorException ex) {
        ProblemDetail problem = ex.getResponseBodyAs(ProblemDetail.class);
        String detail = problem != null && problem.getDetail() != null ? problem.getDetail() : ex.getMessage();
        if (ex.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return new NotFoundException(detail);
        }
        if (ex.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) {
            return new ConflictException(detail);
        }
        return new CatalogUnavailableException(ex);
    }

}
