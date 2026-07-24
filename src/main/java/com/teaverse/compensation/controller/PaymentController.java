package com.teaverse.compensation.controller;

import com.teaverse.compensation.dto.request.CreatePaymentRequest;
import com.teaverse.compensation.dto.request.MoMoPaymentResultRequest;
import com.teaverse.compensation.dto.request.SimulatePaymentRequest;
import com.teaverse.compensation.dto.response.ApiResponse;
import com.teaverse.compensation.dto.response.PaymentResponse;
import com.teaverse.compensation.service.PaymentService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/momo/create")
    public ApiResponse<PaymentResponse> createMoMoPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        return ApiResponse.ok("MoMo payment created", paymentService.createPayment(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable String id) {
        return ApiResponse.ok(paymentService.getPayment(id));
    }

    @GetMapping("/momo/result")
    public ApiResponse<PaymentResponse> momoResult(@RequestParam Map<String, String> params) {
        return ApiResponse.ok("MoMo result processed", paymentService.handleMoMoResult(params));
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody MoMoPaymentResultRequest request) {
        paymentService.handleMoMoIpn(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/momo/simulator/{id}")
    public ApiResponse<PaymentResponse> simulateMoMoPayment(
            @PathVariable String id,
            @Valid @RequestBody SimulatePaymentRequest request
    ) {
        return ApiResponse.ok("MoMo simulator processed", paymentService.simulatePayment(id, request));
    }
}
