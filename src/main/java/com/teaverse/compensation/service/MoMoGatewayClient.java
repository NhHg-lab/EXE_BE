package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.request.MoMoCreatePaymentRequest;
import com.teaverse.compensation.dto.response.MoMoCreatePaymentResponse;

public interface MoMoGatewayClient {
    MoMoCreatePaymentResponse createPayment(MoMoCreatePaymentRequest request);
}
