package com.teaverse.compensation.controller;

import com.teaverse.compensation.dto.response.ApiResponse;
import com.teaverse.compensation.dto.request.CreatePaymentRequest;
import com.teaverse.compensation.dto.response.PaymentResponse;
import com.teaverse.compensation.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/vnpay/create")
    public ApiResponse<PaymentResponse> createVnPayPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok("VNPay payment created", paymentService.createPayment(request, servletRequest));
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable String id) {
        return ApiResponse.ok(paymentService.getPayment(id));
    }

    @GetMapping("/vnpay/return")
    public ApiResponse<Map<String, String>> vnpayReturn(@RequestParam Map<String, String> params) {
        return ApiResponse.ok("VNPay return processed", paymentService.handleVnPayCallback(params));
    }

    @GetMapping("/vnpay/ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        return paymentService.handleVnPayCallback(params);
    }
}
