package com.teaverse.compensation.dto.response;

public record MoMoCreatePaymentResponse(
        String partnerCode,
        String requestId,
        String orderId,
        long amount,
        long responseTime,
        String message,
        int resultCode,
        String payUrl,
        String deeplink,
        String qrCodeUrl,
        String signature
) {
}
