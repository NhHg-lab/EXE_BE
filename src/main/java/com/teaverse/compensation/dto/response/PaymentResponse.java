package com.teaverse.compensation.dto.response;

import com.teaverse.compensation.model.PaymentPurpose;
import com.teaverse.compensation.model.PaymentStatus;
import java.math.BigDecimal;

public record PaymentResponse(
        String id,
        PaymentPurpose purpose,
        String referenceId,
        BigDecimal amount,
        PaymentStatus status,
        String provider,
        String transactionRef,
        String requestId,
        String paymentUrl
) {
}
