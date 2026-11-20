package com.shoplite.orders;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.shoplite.common.UnprocessableException;

/**
 * Makes POST /api/orders safe to retry. A client sends a unique Idempotency-Key header with
 * each new order. If the same key arrives again (a network retry, a double click), ShopLite
 * returns the order it already created instead of creating and charging a second one.
 */
@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    /**
     * The order an earlier request with this key created, if any.
     * The same key with a different body is a client bug, so it is refused (422).
     */
    @Transactional(readOnly = true)
    public Optional<Long> findOrderId(Long customerId, String key, String requestHash) {
        return repository.findByCustomerIdAndKey(customerId, key).map(saved -> {
            if (!saved.getRequestHash().equals(requestHash)) {
                throw new UnprocessableException("Idempotency-Key " + key
                        + " was already used with a different request body");
            }
            return saved.getOrderId();
        });
    }

    /**
     * Saves the key in the same transaction as the new order. If two requests with the
     * same key race, the unique constraint rejects the second one, its whole transaction
     * (order and stock change included) rolls back, and the client gets 409.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void remember(Long customerId, String key, String requestHash, Long orderId) {
        // saveAndFlush sends the INSERT now, whatever the id strategy, so a duplicate fails
        // here, inside the order's transaction, and not later at commit.
        repository.saveAndFlush(new IdempotencyKey(customerId, key, requestHash, orderId));
    }

    /** SHA-256 of a request fingerprint, as 64 hex characters. */
    public static String hash(String fingerprint) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            // Every Java runtime must support SHA-256, so this cannot happen.
            throw new IllegalStateException(ex);
        }
    }

}
