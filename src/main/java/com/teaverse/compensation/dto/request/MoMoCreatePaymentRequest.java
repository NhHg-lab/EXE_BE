package com.teaverse.compensation.dto.request;

public record MoMoCreatePaymentRequest(
        String partnerCode,
        String requestId,
        long amount,
        String orderId,
        String orderInfo,
        String redirectUrl,
        String ipnUrl,
        String requestType,
        String extraData,
        String lang,
        boolean autoCapture,
        String signature
) {
}
