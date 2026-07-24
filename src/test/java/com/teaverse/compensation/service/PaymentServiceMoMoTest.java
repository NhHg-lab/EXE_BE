package com.teaverse.compensation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.teaverse.compensation.dto.request.MoMoCreatePaymentRequest;
import com.teaverse.compensation.dto.request.MoMoPaymentResultRequest;
import com.teaverse.compensation.dto.request.SimulatePaymentRequest;
import com.teaverse.compensation.dto.response.MoMoCreatePaymentResponse;
import com.teaverse.compensation.dto.response.PaymentResponse;
import com.teaverse.compensation.exception.BadRequestException;
import com.teaverse.compensation.mapper.DtoMapper;
import com.teaverse.compensation.model.Payment;
import com.teaverse.compensation.model.PaymentPurpose;
import com.teaverse.compensation.model.PaymentStatus;
import com.teaverse.compensation.model.User;
import com.teaverse.compensation.repository.PaymentRepository;
import com.teaverse.compensation.repository.UserRepository;
import com.teaverse.compensation.util.MoMoSignatureUtil;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PaymentServiceMoMoTest {
    private static final String PARTNER_CODE = "TEST_PARTNER";
    private static final String ACCESS_KEY = "test-access-key";
    private static final String SECRET_KEY = "test-secret-key";
    private static final String REDIRECT_URL = "http://localhost:3000/momo-result";
    private static final String IPN_URL = "https://example.com/api/v1/payments/momo/ipn";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPaymentSignsDocumentedPayloadAndStoresPayUrl() {
        PaymentRepository repository = mock(PaymentRepository.class);
        MoMoGatewayClient client = mock(MoMoGatewayClient.class);
        AtomicReference<MoMoCreatePaymentRequest> captured = new AtomicReference<>();
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(client.createPayment(any(MoMoCreatePaymentRequest.class))).thenAnswer(invocation -> {
            MoMoCreatePaymentRequest request = invocation.getArgument(0);
            captured.set(request);
            long responseTime = 1_721_720_619_912L;
            String payUrl = "https://test-payment.momo.vn/v2/gateway/pay?t=test";
            String responseData = MoMoSignatureUtil.createResponseData(
                    ACCESS_KEY,
                    request.amount(),
                    "Successful.",
                    request.orderId(),
                    request.partnerCode(),
                    payUrl,
                    request.requestId(),
                    responseTime,
                    0
            );
            return new MoMoCreatePaymentResponse(
                    request.partnerCode(),
                    request.requestId(),
                    request.orderId(),
                    request.amount(),
                    responseTime,
                    "Successful.",
                    0,
                    payUrl,
                    "momo://test",
                    "qr-data",
                    MoMoSignatureUtil.hmacSha256(SECRET_KEY, responseData)
            );
        });

        PaymentService service = createService(repository, client);
        User user = new User();
        user.setId("user-1");

        Payment payment = service.createPayment(
                user,
                PaymentPurpose.PREMIUM_PLAN,
                "premium-plan",
                new BigDecimal("15000"),
                "GameTrust premium"
        );

        MoMoCreatePaymentRequest request = captured.get();
        String rawData = MoMoSignatureUtil.createRequestData(
                ACCESS_KEY,
                request.amount(),
                request.extraData(),
                request.ipnUrl(),
                request.orderId(),
                request.orderInfo(),
                request.partnerCode(),
                request.redirectUrl(),
                request.requestId(),
                request.requestType()
        );
        assertThat(request.signature()).isEqualTo(MoMoSignatureUtil.hmacSha256(SECRET_KEY, rawData));
        assertThat(request.amount()).isEqualTo(15000);
        assertThat(request.requestType()).isEqualTo("captureWallet");
        assertThat(payment.getProvider()).isEqualTo("MOMO");
        assertThat(payment.getPaymentUrl()).startsWith("https://test-payment.momo.vn/");
    }

    @Test
    void validIpnMarksPaymentPaid() {
        PaymentRepository repository = mock(PaymentRepository.class);
        Payment payment = pendingPayment();
        when(repository.findByTransactionRef(payment.getTransactionRef())).thenReturn(Optional.of(payment));
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentService service = createService(repository, mock(MoMoGatewayClient.class));
        MoMoPaymentResultRequest result = signedResult(payment, 15000, 0);

        service.handleMoMoIpn(result);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getProviderPayload()).containsEntry("transId", "4088878653");
    }

    @Test
    void invalidIpnSignatureDoesNotChangePaymentStatus() {
        PaymentRepository repository = mock(PaymentRepository.class);
        Payment payment = pendingPayment();
        when(repository.findByTransactionRef(payment.getTransactionRef())).thenReturn(Optional.of(payment));
        PaymentService service = createService(repository, mock(MoMoGatewayClient.class));
        MoMoPaymentResultRequest valid = signedResult(payment, 15000, 0);
        MoMoPaymentResultRequest invalid = new MoMoPaymentResultRequest(
                valid.partnerCode(),
                valid.orderId(),
                valid.requestId(),
                valid.amount(),
                valid.orderInfo(),
                valid.orderType(),
                valid.transId(),
                valid.resultCode(),
                valid.message(),
                valid.payType(),
                valid.responseTime(),
                valid.extraData(),
                "invalid-signature"
        );

        assertThatThrownBy(() -> service.handleMoMoIpn(invalid))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid MoMo payment signature");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void missingCredentialsUseLocalSimulatorWithoutCallingMoMo() {
        PaymentRepository repository = mock(PaymentRepository.class);
        MoMoGatewayClient client = mock(MoMoGatewayClient.class);
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId("payment-1");
            }
            return payment;
        });
        PaymentService service = createService(
                repository,
                client,
                "",
                "",
                "",
                true
        );
        User user = new User();
        user.setId("user-1");

        Payment payment = service.createPayment(
                user,
                PaymentPurpose.MARKETPLACE_ORDER,
                "listing-1",
                new BigDecimal("15000"),
                "Simulator test"
        );

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getProvider()).isEqualTo("MOMO");
        assertThat(payment.getProviderPayload()).containsEntry("mode", "SIMULATOR");
        assertThat(payment.getPaymentUrl())
                .isEqualTo("http://localhost:3000/momo-simulator?paymentId=payment-1");
        verifyNoInteractions(client);
    }

    @Test
    void simulatorSuccessMarksOnlyOwnedPaymentPaid() {
        PaymentRepository repository = mock(PaymentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = new User();
        user.setId("user-1");
        user.setEmail("gamer@gametrust.dev");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );

        Payment payment = pendingPayment();
        payment.setId("payment-1");
        payment.setProviderPayload(java.util.Map.of("mode", "SIMULATOR"));
        when(repository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentService service = new PaymentService(
                repository,
                null,
                null,
                userRepository,
                new CurrentUserService(userRepository),
                null,
                new DtoMapper(),
                mock(MoMoGatewayClient.class),
                "",
                "",
                "",
                REDIRECT_URL,
                IPN_URL,
                "captureWallet",
                "vi",
                true,
                "http://localhost:3000/momo-simulator"
        );

        PaymentResponse response = service.simulatePayment(
                payment.getId(),
                new SimulatePaymentRequest("SUCCESS")
        );

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getProviderPayload()).containsEntry("simulatedResult", "SUCCESS");
    }

    private PaymentService createService(PaymentRepository repository, MoMoGatewayClient client) {
        return createService(
                repository,
                client,
                PARTNER_CODE,
                ACCESS_KEY,
                SECRET_KEY,
                false
        );
    }

    private PaymentService createService(
            PaymentRepository repository,
            MoMoGatewayClient client,
            String partnerCode,
            String accessKey,
            String secretKey,
            boolean simulatorEnabled
    ) {
        return new PaymentService(
                repository,
                null,
                null,
                null,
                null,
                null,
                null,
                client,
                partnerCode,
                accessKey,
                secretKey,
                REDIRECT_URL,
                IPN_URL,
                "captureWallet",
                "vi",
                simulatorEnabled,
                "http://localhost:3000/momo-simulator"
        );
    }

    private Payment pendingPayment() {
        Payment payment = new Payment();
        payment.setUserId("user-1");
        payment.setPurpose(PaymentPurpose.MARKETPLACE_ORDER);
        payment.setReferenceId("listing-1");
        payment.setAmount(new BigDecimal("15000"));
        payment.setTransactionRef("GT-123");
        payment.setProviderRequestId("REQ-123");
        return payment;
    }

    private MoMoPaymentResultRequest signedResult(Payment payment, long amount, int resultCode) {
        long transId = 4_088_878_653L;
        long responseTime = 1_721_720_663_942L;
        String message = resultCode == 0 ? "Successful." : "Payment failed.";
        String rawData = MoMoSignatureUtil.paymentResultData(
                ACCESS_KEY,
                amount,
                "",
                message,
                payment.getTransactionRef(),
                "GameTrust order",
                "momo_wallet",
                PARTNER_CODE,
                "qr",
                payment.getProviderRequestId(),
                responseTime,
                resultCode,
                transId
        );
        return new MoMoPaymentResultRequest(
                PARTNER_CODE,
                payment.getTransactionRef(),
                payment.getProviderRequestId(),
                amount,
                "GameTrust order",
                "momo_wallet",
                transId,
                resultCode,
                message,
                "qr",
                responseTime,
                "",
                MoMoSignatureUtil.hmacSha256(SECRET_KEY, rawData)
        );
    }
}
