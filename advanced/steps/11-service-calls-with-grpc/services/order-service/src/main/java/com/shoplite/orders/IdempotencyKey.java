package com.shoplite.orders;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row per Idempotency-Key a customer has used: which request it carried and which order it created. */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyKey() {
    }

    IdempotencyKey(Long customerId, String key, String requestHash, Long orderId) {
        this.customerId = customerId;
        this.key = key;
        this.requestHash = requestHash;
        this.orderId = orderId;
        this.createdAt = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Long getOrderId() {
        return orderId;
    }

}
