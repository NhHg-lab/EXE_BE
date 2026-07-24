package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.request.CreatePaymentRequest;
import com.teaverse.compensation.dto.request.MoMoCreatePaymentRequest;
import com.teaverse.compensation.dto.request.MoMoPaymentResultRequest;
import com.teaverse.compensation.dto.request.SimulatePaymentRequest;
import com.teaverse.compensation.dto.response.MoMoCreatePaymentResponse;
import com.teaverse.compensation.dto.response.PaymentResponse;
import com.teaverse.compensation.exception.BadRequestException;
import com.teaverse.compensation.exception.NotFoundException;
import com.teaverse.compensation.mapper.DtoMapper;
import com.teaverse.compensation.model.NotificationType;
import com.teaverse.compensation.model.Payment;
import com.teaverse.compensation.model.PaymentPurpose;
import com.teaverse.compensation.model.PaymentStatus;
import com.teaverse.compensation.model.RegistrationStatus;
import com.teaverse.compensation.model.Tournament;
import com.teaverse.compensation.model.TournamentRegistration;
import com.teaverse.compensation.model.User;
import com.teaverse.compensation.repository.PaymentRepository;
import com.teaverse.compensation.repository.TournamentRegistrationRepository;
import com.teaverse.compensation.repository.TournamentRepository;
import com.teaverse.compensation.repository.UserRepository;
import com.teaverse.compensation.util.MoMoSignatureUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private static final long MOMO_MIN_AMOUNT = 1_000L;
    private static final long MOMO_MAX_AMOUNT = 50_000_000L;
    private static final String EMPTY_EXTRA_DATA = "";

    private final PaymentRepository paymentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final DtoMapper mapper;
    private final MoMoGatewayClient momoClient;
    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final String redirectUrl;
    private final String ipnUrl;
    private final String requestType;
    private final String lang;
    private final boolean simulatorEnabled;
    private final String simulatorUrl;

    public PaymentService(
            PaymentRepository paymentRepository,
            TournamentRegistrationRepository registrationRepository,
            TournamentRepository tournamentRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            NotificationService notificationService,
            DtoMapper mapper,
            MoMoGatewayClient momoClient,
            @Value("${app.momo.partner-code}") String partnerCode,
            @Value("${app.momo.access-key}") String accessKey,
            @Value("${app.momo.secret-key}") String secretKey,
            @Value("${app.momo.redirect-url}") String redirectUrl,
            @Value("${app.momo.ipn-url}") String ipnUrl,
            @Value("${app.momo.request-type:captureWallet}") String requestType,
            @Value("${app.momo.lang:vi}") String lang,
            @Value("${app.momo.simulator-enabled:false}") boolean simulatorEnabled,
            @Value("${app.momo.simulator-url:http://localhost:3000/momo-simulator}") String simulatorUrl
    ) {
        this.paymentRepository = paymentRepository;
        this.registrationRepository = registrationRepository;
        this.tournamentRepository = tournamentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.mapper = mapper;
        this.momoClient = momoClient;
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.redirectUrl = redirectUrl;
        this.ipnUrl = ipnUrl;
        this.requestType = requestType;
        this.lang = lang;
        this.simulatorEnabled = simulatorEnabled;
        this.simulatorUrl = simulatorUrl;
    }

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        User user = currentUserService.getCurrentUser();
        return mapper.toPaymentResponse(createPayment(
                user,
                request.purpose(),
                request.referenceId(),
                request.amount(),
                request.orderInfo()
        ));
    }

    public Payment createPayment(
            User user,
            PaymentPurpose purpose,
            String referenceId,
            BigDecimal amount,
            String orderInfo
    ) {
        long momoAmount = toMoMoAmount(amount);
        String normalizedOrderInfo = normalizeOrderInfo(orderInfo);
        String orderId = "GT-" + compactUuid();
        String requestId = "REQ-" + compactUuid();
        boolean simulatorMode = shouldUseSimulator();
        if (!simulatorMode) {
            ensureConfigured();
        }

        Payment payment = new Payment();
        payment.setUserId(user.getId());
        payment.setPurpose(purpose);
        payment.setReferenceId(referenceId);
        payment.setAmount(BigDecimal.valueOf(momoAmount));
        payment.setProvider("MOMO");
        payment.setTransactionRef(orderId);
        payment.setProviderRequestId(requestId);
        payment = paymentRepository.save(payment);

        if (simulatorMode) {
            return createSimulatorPayment(payment);
        }

        String rawSignature = MoMoSignatureUtil.createRequestData(
                accessKey,
                momoAmount,
                EMPTY_EXTRA_DATA,
                ipnUrl,
                orderId,
                normalizedOrderInfo,
                partnerCode,
                redirectUrl,
                requestId,
                requestType
        );
        MoMoCreatePaymentRequest momoRequest = new MoMoCreatePaymentRequest(
                partnerCode,
                requestId,
                momoAmount,
                orderId,
                normalizedOrderInfo,
                redirectUrl,
                ipnUrl,
                requestType,
                EMPTY_EXTRA_DATA,
                lang,
                true,
                MoMoSignatureUtil.hmacSha256(secretKey, rawSignature)
        );

        try {
            MoMoCreatePaymentResponse momoResponse = momoClient.createPayment(momoRequest);
            validateCreateResponse(momoRequest, momoResponse);
            payment.setProviderPayload(toPayload(momoResponse));
            if (momoResponse.resultCode() != 0 || isBlank(momoResponse.payUrl())) {
                markFailed(payment, payment.getProviderPayload());
                throw new BadRequestException("MoMo rejected the payment: " + safeMessage(momoResponse.message()));
            }
            payment.setPaymentUrl(momoResponse.payUrl());
            return paymentRepository.save(payment);
        } catch (RuntimeException ex) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                markFailed(payment, Map.of("error", safeMessage(ex.getMessage())));
            }
            throw ex;
        }
    }

    public PaymentResponse getPayment(String id) {
        User user = currentUserService.getCurrentUser();
        return paymentRepository.findById(id)
                .filter(item -> user.getId().equals(item.getUserId()))
                .map(mapper::toPaymentResponse)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    public PaymentResponse handleMoMoResult(Map<String, String> params) {
        return mapper.toPaymentResponse(processMoMoResult(parseResult(params)));
    }

    public void handleMoMoIpn(MoMoPaymentResultRequest request) {
        processMoMoResult(request);
    }

    public synchronized PaymentResponse simulatePayment(String id, SimulatePaymentRequest request) {
        if (!simulatorEnabled) {
            throw new BadRequestException("MoMo simulator is disabled");
        }
        User user = currentUserService.getCurrentUser();
        Payment payment = paymentRepository.findById(id)
                .filter(item -> user.getId().equals(item.getUserId()))
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        if (!"SIMULATOR".equals(payment.getProviderPayload().get("mode"))) {
            throw new BadRequestException("Payment was not created by the MoMo simulator");
        }

        String action = request == null ? null : request.action();
        if ("SUCCESS".equals(action)) {
            if (payment.getStatus() == PaymentStatus.CANCELLED || payment.getStatus() == PaymentStatus.FAILED) {
                throw new BadRequestException("A cancelled or failed payment cannot be completed");
            }
            Map<String, String> payload = new HashMap<>(payment.getProviderPayload());
            payload.put("simulatedResult", "SUCCESS");
            payload.put("completedAt", Instant.now().toString());
            markPaid(payment, payload);
        } else if ("CANCEL".equals(action)) {
            if (payment.getStatus() == PaymentStatus.PAID) {
                throw new BadRequestException("A paid payment cannot be cancelled");
            }
            payment.setStatus(PaymentStatus.CANCELLED);
            Map<String, String> payload = new HashMap<>(payment.getProviderPayload());
            payload.put("simulatedResult", "CANCEL");
            payload.put("completedAt", Instant.now().toString());
            payment.setProviderPayload(payload);
            paymentRepository.save(payment);
        } else {
            throw new BadRequestException("Simulator action must be SUCCESS or CANCEL");
        }
        return mapper.toPaymentResponse(payment);
    }

    private synchronized Payment processMoMoResult(MoMoPaymentResultRequest result) {
        ensureConfigured();
        validateRequiredResultFields(result);
        Payment payment = paymentRepository.findByTransactionRef(result.orderId())
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        validatePaymentResult(payment, result);

        Map<String, String> payload = toPayload(result);
        if (result.resultCode() == 0) {
            markPaid(payment, payload);
        } else if (payment.getStatus() != PaymentStatus.PAID) {
            payment.setStatus(result.resultCode() == 1006 ? PaymentStatus.CANCELLED : PaymentStatus.FAILED);
            payment.setProviderPayload(payload);
            paymentRepository.save(payment);
        }
        return payment;
    }

    private void validateCreateResponse(
            MoMoCreatePaymentRequest request,
            MoMoCreatePaymentResponse response
    ) {
        if (!partnerCode.equals(response.partnerCode())
                || !request.orderId().equals(response.orderId())
                || !request.requestId().equals(response.requestId())
                || request.amount() != response.amount()) {
            throw new BadRequestException("MoMo create-payment response does not match the request");
        }
        String rawSignature = MoMoSignatureUtil.createResponseData(
                accessKey,
                response.amount(),
                safeValue(response.message()),
                response.orderId(),
                response.partnerCode(),
                safeValue(response.payUrl()),
                response.requestId(),
                response.responseTime(),
                response.resultCode()
        );
        String expected = MoMoSignatureUtil.hmacSha256(secretKey, rawSignature);
        if (!MoMoSignatureUtil.matches(expected, response.signature())) {
            throw new BadRequestException("Invalid MoMo create-payment response signature");
        }
    }

    private void validatePaymentResult(Payment payment, MoMoPaymentResultRequest result) {
        if (!partnerCode.equals(result.partnerCode())) {
            throw new BadRequestException("Invalid MoMo partner code");
        }
        if (!result.requestId().equals(payment.getProviderRequestId())) {
            throw new BadRequestException("Invalid MoMo request ID");
        }
        if (toMoMoAmount(payment.getAmount()) != result.amount()) {
            throw new BadRequestException("Invalid MoMo payment amount");
        }
        String rawSignature = MoMoSignatureUtil.paymentResultData(
                accessKey,
                result.amount(),
                safeValue(result.extraData()),
                safeValue(result.message()),
                result.orderId(),
                safeValue(result.orderInfo()),
                safeValue(result.orderType()),
                result.partnerCode(),
                safeValue(result.payType()),
                result.requestId(),
                result.responseTime(),
                result.resultCode(),
                result.transId()
        );
        String expected = MoMoSignatureUtil.hmacSha256(secretKey, rawSignature);
        if (!MoMoSignatureUtil.matches(expected, result.signature())) {
            throw new BadRequestException("Invalid MoMo payment signature");
        }
    }

    private void validateRequiredResultFields(MoMoPaymentResultRequest result) {
        if (result == null
                || isBlank(result.partnerCode())
                || isBlank(result.orderId())
                || isBlank(result.requestId())
                || isBlank(result.signature())) {
            throw new BadRequestException("MoMo result data is incomplete");
        }
    }

    private MoMoPaymentResultRequest parseResult(Map<String, String> params) {
        try {
            return new MoMoPaymentResultRequest(
                    required(params, "partnerCode"),
                    required(params, "orderId"),
                    required(params, "requestId"),
                    Long.parseLong(required(params, "amount")),
                    params.getOrDefault("orderInfo", ""),
                    params.getOrDefault("orderType", ""),
                    Long.parseLong(params.getOrDefault("transId", "0")),
                    Integer.parseInt(required(params, "resultCode")),
                    params.getOrDefault("message", ""),
                    params.getOrDefault("payType", ""),
                    Long.parseLong(required(params, "responseTime")),
                    params.getOrDefault("extraData", ""),
                    required(params, "signature")
            );
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid numeric value in MoMo result");
        }
    }

    private void markFailed(Payment payment, Map<String, String> payload) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
        payment.setStatus(PaymentStatus.FAILED);
        payment.setProviderPayload(new HashMap<>(payload));
        paymentRepository.save(payment);
    }

    private void markPaid(Payment payment, Map<String, String> payload) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        payment.setProviderPayload(new HashMap<>(payload));
        paymentRepository.save(payment);

        if (payment.getPurpose() == PaymentPurpose.TOURNAMENT_ENTRY) {
            TournamentRegistration registration = registrationRepository.findById(payment.getReferenceId())
                    .orElseThrow(() -> new NotFoundException("Tournament registration not found"));
            registration.setStatus(RegistrationStatus.REGISTERED);
            registrationRepository.save(registration);

            Tournament tournament = tournamentRepository.findById(registration.getTournamentId())
                    .orElseThrow(() -> new NotFoundException("Tournament not found"));
            tournament.setRegisteredTeams(Math.min(tournament.getMaxTeams(), tournament.getRegisteredTeams() + 1));
            tournamentRepository.save(tournament);

            notificationService.create(
                    registration.getUserId(),
                    NotificationType.PAYMENT,
                    "Tournament payment confirmed",
                    "Your payment for " + tournament.getTitle() + " is confirmed. Registration is now active."
            );
        } else if (payment.getPurpose() == PaymentPurpose.PREMIUM_PLAN) {
            userRepository.findById(payment.getUserId()).ifPresent(user -> {
                user.setPremium(true);
                userRepository.save(user);
                notificationService.create(
                        user.getId(),
                        NotificationType.PAYMENT,
                        "Premium activated",
                        "Your GameTrust premium plan is now active."
                );
            });
        }
    }

    private Map<String, String> toPayload(MoMoCreatePaymentResponse response) {
        Map<String, String> payload = new HashMap<>();
        payload.put("partnerCode", safeValue(response.partnerCode()));
        payload.put("requestId", safeValue(response.requestId()));
        payload.put("orderId", safeValue(response.orderId()));
        payload.put("amount", String.valueOf(response.amount()));
        payload.put("responseTime", String.valueOf(response.responseTime()));
        payload.put("message", safeValue(response.message()));
        payload.put("resultCode", String.valueOf(response.resultCode()));
        payload.put("payUrl", safeValue(response.payUrl()));
        payload.put("deeplink", safeValue(response.deeplink()));
        payload.put("qrCodeUrl", safeValue(response.qrCodeUrl()));
        return payload;
    }

    private Map<String, String> toPayload(MoMoPaymentResultRequest result) {
        Map<String, String> payload = new HashMap<>();
        payload.put("partnerCode", safeValue(result.partnerCode()));
        payload.put("orderId", safeValue(result.orderId()));
        payload.put("requestId", safeValue(result.requestId()));
        payload.put("amount", String.valueOf(result.amount()));
        payload.put("orderInfo", safeValue(result.orderInfo()));
        payload.put("orderType", safeValue(result.orderType()));
        payload.put("transId", String.valueOf(result.transId()));
        payload.put("resultCode", String.valueOf(result.resultCode()));
        payload.put("message", safeValue(result.message()));
        payload.put("payType", safeValue(result.payType()));
        payload.put("responseTime", String.valueOf(result.responseTime()));
        payload.put("extraData", safeValue(result.extraData()));
        return payload;
    }

    private void ensureConfigured() {
        if (isBlank(partnerCode) || isBlank(accessKey) || isBlank(secretKey)) {
            throw new BadRequestException(
                    "MoMo is not configured. Set MOMO_PARTNER_CODE, MOMO_ACCESS_KEY and MOMO_SECRET_KEY"
            );
        }
    }

    private Payment createSimulatorPayment(Payment payment) {
        Map<String, String> payload = new HashMap<>();
        payload.put("mode", "SIMULATOR");
        payload.put("provider", "MOMO");
        payload.put("message", "Local MoMo payment simulation");
        payment.setProviderPayload(payload);
        String paymentId = URLEncoder.encode(payment.getId(), StandardCharsets.UTF_8);
        payment.setPaymentUrl(simulatorUrl + "?paymentId=" + paymentId);
        return paymentRepository.save(payment);
    }

    private boolean shouldUseSimulator() {
        return simulatorEnabled
                && isBlank(partnerCode)
                && isBlank(accessKey)
                && isBlank(secretKey);
    }

    private long toMoMoAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Payment amount is required");
        }
        final long value;
        try {
            value = amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException ex) {
            throw new BadRequestException("MoMo payment amount must be an integer VND value");
        }
        if (value < MOMO_MIN_AMOUNT || value > MOMO_MAX_AMOUNT) {
            throw new BadRequestException("MoMo payment amount must be between 1,000 and 50,000,000 VND");
        }
        return value;
    }

    private String normalizeOrderInfo(String orderInfo) {
        String value = isBlank(orderInfo) ? "GameTrust payment" : orderInfo.trim();
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    private String required(Map<String, String> params, String key) {
        String value = params.get(key);
        if (isBlank(value)) {
            throw new BadRequestException("Missing MoMo result field: " + key);
        }
        return value;
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String safeMessage(String value) {
        return isBlank(value) ? "Unknown payment gateway error" : value;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
