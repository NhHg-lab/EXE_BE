package com.teaverse.compensation.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ListingResponse(
        String id,
        String sellerId,
        String title,
        String game,
        String server,
        String rankBadge,
        String description,
        BigDecimal price,
        double trustScore,
        boolean active,
        Instant createdAt
) {
}
