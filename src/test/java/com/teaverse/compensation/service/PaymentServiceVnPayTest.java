package com.teaverse.compensation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.teaverse.compensation.model.Payment;
import com.teaverse.compensation.model.PaymentStatus;
import com.teaverse.compensation.repository.PaymentRepository;
import com.teaverse.compensation.util.VnPayUtil;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentServiceVnPayTest {
    private static final String TEST_SECRET = "sandbox-test-secret";
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Test
    void generatedUrlUsesVnPayAmountDateSortingAndHmacSha512Format() {
        PaymentService service = new PaymentService(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "TESTTMN",
                TEST_SECRET,
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/vnpay-return"
        );
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("125000"));
        payment.setTransactionRef("GT123456789");

        String paymentUrl = ReflectionTestUtils.invokeMethod(
                service,
                "buildPaymentUrl",
                payment,
                "Tournament entry #42",
                "127.0.0.1"
        );

        assertThat(paymentUrl).isNotNull();
        URI uri = URI.create(paymentUrl);
        String rawQuery = uri.getRawQuery();
        String hashMarker = "&vnp_SecureHash=";
        int hashIndex = rawQuery.lastIndexOf(hashMarker);
        String signedQuery = rawQuery.substring(0, hashIndex);
        String secureHash = rawQuery.substring(hashIndex + hashMarker.length());
        Map<String, String> params = decodeQuery(signedQuery);

        assertThat(params.get("vnp_Amount")).isEqualTo("12500000");
        assertThat(params.get("vnp_Amount")).matches("\\d+");
        assertThat(params.get("vnp_CreateDate")).matches("\\d{14}");
        assertThat(params.get("vnp_ExpireDate")).matches("\\d{14}");
        assertThat(params.get("vnp_ReturnUrl")).isEqualTo("http://localhost:3000/vnpay-return");

        LocalDateTime createdAt = LocalDateTime.parse(params.get("vnp_CreateDate"), VNPAY_DATE_FORMAT);
        LocalDateTime expiresAt = LocalDateTime.parse(params.get("vnp_ExpireDate"), VNPAY_DATE_FORMAT);
        assertThat(expiresAt).isEqualTo(createdAt.plusMinutes(15));

        List<String> keys = params.keySet().stream().toList();
        assertThat(keys).isEqualTo(keys.stream().sorted().toList());
        assertThat(secureHash).matches("[0-9a-f]{128}");
        assertThat(secureHash).isEqualTo(VnPayUtil.hmacSha512(TEST_SECRET, signedQuery));
    }

    @Test
    void callbackRejectsSignedPayloadWhenAmountDoesNotMatchPayment() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentService service = new PaymentService(
                paymentRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                "TESTTMN",
                TEST_SECRET,
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/vnpay-return"
        );
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("125000"));
        payment.setTransactionRef("GT123456789");
        when(paymentRepository.findByTransactionRef("GT123456789")).thenReturn(Optional.of(payment));

        Map<String, String> callback = new TreeMap<>();
        callback.put("vnp_Amount", "10000");
        callback.put("vnp_ResponseCode", "00");
        callback.put("vnp_TransactionStatus", "00");
        callback.put("vnp_TxnRef", "GT123456789");
        callback.put("vnp_SecureHash", VnPayUtil.hmacSha512(TEST_SECRET, VnPayUtil.buildHashData(callback)));

        Map<String, String> result = service.handleVnPayCallback(callback);

        assertThat(result.get("RspCode")).isEqualTo("04");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    private Map<String, String> decodeQuery(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        Arrays.stream(rawQuery.split("&"))
                .map(pair -> pair.split("=", 2))
                .forEach(pair -> params.put(
                        URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                ));
        return params;
    }
}
