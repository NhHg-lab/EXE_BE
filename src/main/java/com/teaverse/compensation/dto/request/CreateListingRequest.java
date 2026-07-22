package com.teaverse.compensation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateListingRequest(
        @NotBlank String title,
        @NotBlank String game,
        String server,
        String rankBadge,
        String description,
        @DecimalMin("0.0") BigDecimal price
) {
}
