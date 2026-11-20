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
 */
@Component
class StockReleaseListener {

    private static final Logger log = LoggerFactory.getLogger(StockReleaseListener.class);

    private final ProductService productService;

    StockReleaseListener(ProductService productService) {
        this.productService = productService;
    }

    @KafkaListener(topics = OrderStockMessage.TOPIC, groupId = "catalog-stock",
            properties = "spring.json.value.default.type=com.shoplite.catalog.OrderStockMessage")
    void on(OrderStockMessage message) {
        if (!message.releasesStock()) {
            return;
        }
        productService.releaseStock(message.lines());
        log.info("Released stock for order {} after {}: {}", message.orderId(), message.type(), message.lines());
    }

}
