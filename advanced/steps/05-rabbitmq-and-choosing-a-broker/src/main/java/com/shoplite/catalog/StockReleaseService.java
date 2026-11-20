package com.shoplite.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Puts stock back for an order event, at most once per event. */
@Service
class StockReleaseService {

    private final ProcessedEvents processedEvents;
    private final ProductService productService;

    StockReleaseService(ProcessedEvents processedEvents, ProductService productService) {
        this.processedEvents = processedEvents;
        this.productService = productService;
    }

    /** One transaction: remember the event and change the stock, or do neither. */
    @Transactional
    boolean releaseOnce(UUID eventId, List<StockLine> lines) {
        if (!processedEvents.firstTime(eventId)) {
            return false;
        }
        productService.releaseStock(lines);
        return true;
    }

}
