package com.shoplite.orders;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Interface projection for "my orders" lists. Spring Data implements it and
 * selects only these five columns: no lines, no customer, no entity tracking.
 */
public interface OrderSummary {

    Long getId();

    OrderStatus getStatus();

    BigDecimal getTotal();

    String getCurrency();

    Instant getCreatedAt();

}
