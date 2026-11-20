package com.shoplite.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Puts stock back when an order's payment fails or an admin cancels it.
 *
 * groupId "catalog-stock": Kafka remembers how far this group has read (its offset), so after
 * a restart the listener carries on where it stopped. The JSON is read into OrderStockMessage,
 * the catalog's own record, because the producer sends no type header.
 * Messages can arrive more than once, so the release goes through StockReleaseService, which
 * applies each event id only once.
 */
@Component
class StockReleaseListener {

    private static final Logger log = LoggerFactory.getLogger(StockReleaseListener.class);

    private final StockReleaseService stockReleaseService;

    StockReleaseListener(StockReleaseService stockReleaseService) {
        this.stockReleaseService = stockReleaseService;
    }

    @KafkaListener(topics = OrderStockMessage.TOPIC, groupId = "catalog-stock",
            properties = "spring.json.value.default.type=com.shoplite.catalog.OrderStockMessage")
    void on(OrderStockMessage message) {
        if (!message.releasesStock()) {
            return;
        }
        if (stockReleaseService.releaseOnce(message.eventId(), message.lines())) {
            log.info("Released stock for order {} after {}: {}", message.orderId(), message.type(), message.lines());
        } else {
            log.info("Skipped duplicate event {} for order {}", message.eventId(), message.orderId());
        }
    }

}
