package com.shoplite.payments;

import java.math.BigDecimal;

/** The JSON body ShopLite sends to the payment provider. */
public record ChargeRequest(String reference, BigDecimal amount, String currency) {
}
