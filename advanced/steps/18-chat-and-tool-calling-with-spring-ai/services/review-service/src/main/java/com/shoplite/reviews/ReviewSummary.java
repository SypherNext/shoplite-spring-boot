package com.shoplite.reviews;

import java.math.BigDecimal;

public record ReviewSummary(long reviews, BigDecimal average) {
}
