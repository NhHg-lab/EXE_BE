package com.teaverse.compensation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SimulatePaymentRequest(
        @NotBlank
        @Pattern(regexp = "SUCCESS|CANCEL")
        String action
) {
}
