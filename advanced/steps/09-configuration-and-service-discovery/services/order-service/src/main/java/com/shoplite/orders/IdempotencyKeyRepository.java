package com.shoplite.orders;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByCustomerIdAndKey(Long customerId, String key);

    // One DELETE statement, instead of loading every old row and deleting it one by one.
    @Modifying
    @Query("delete from IdempotencyKey k where k.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);

}
