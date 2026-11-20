package com.shoplite.orders;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByCustomerIdAndKey(Long customerId, String key);

}
