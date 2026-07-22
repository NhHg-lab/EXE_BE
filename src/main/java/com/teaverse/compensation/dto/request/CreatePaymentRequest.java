package com.teaverse.compensation.dto.request;

import com.teaverse.compensation.model.PaymentPurpose;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull PaymentPurpose purpose,
        @NotBlank String referenceId,
        @DecimalMin("0.0") BigDecimal amount,
        String orderInfo
) {
}
