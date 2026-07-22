package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.request.CreatePaymentRequest;
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
import com.teaverse.compensation.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private static final DateTimeFormatter VNPAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final PaymentRepository paymentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final DtoMapper mapper;
    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String returnUrl;

    public PaymentService(
            PaymentRepository paymentRepository,
            TournamentRegistrationRepository registrationRepository,
            TournamentRepository tournamentRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            NotificationService notificationService,
            DtoMapper mapper,
            @Value("${app.vnpay.tmn-code}") String tmnCode,
            @Value("${app.vnpay.hash-secret}") String hashSecret,
            @Value("${app.vnpay.pay-url}") String payUrl,
            @Value("${app.vnpay.return-url}") String returnUrl
    ) {
        this.paymentRepository = paymentRepository;
        this.registrationRepository = registrationRepository;
        this.tournamentRepository = tournamentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.mapper = mapper;
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
        this.payUrl = payUrl;
        this.returnUrl = returnUrl;
    }

    public PaymentResponse createPayment(CreatePaymentRequest request, HttpServletRequest servletRequest) {
        User user = currentUserService.getCurrentUser();
        Payment payment = createPayment(
                user,
                request.purpose(),
                request.referenceId(),
                request.amount(),
                request.orderInfo(),
                getClientIp(servletRequest)
        );
        return mapper.toPaymentResponse(payment);
    }

    public Payment createPayment(
            User user,
            PaymentPurpose purpose,
            String referenceId,
            BigDecimal amount,
            String orderInfo,
            String clientIp
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Payment amount must be greater than or equal to zero");
        }

        Payment payment = new Payment();
        payment.setUserId(user.getId());
        payment.setPurpose(purpose);
        payment.setReferenceId(referenceId);
        payment.setAmount(amount);
        payment.setTransactionRef("GT" + System.currentTimeMillis());
        payment = paymentRepository.save(payment);

        payment.setPaymentUrl(buildPaymentUrl(payment, orderInfo, clientIp));
        return paymentRepository.save(payment);
    }

    public PaymentResponse getPayment(String id) {
        return paymentRepository.findById(id)
                .map(mapper::toPaymentResponse)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    public Map<String, String> handleVnPayCallback(Map<String, String> params) {
        String transactionRef = params.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (isConfiguredForRealVnPay() && !verifySignature(params)) {
            markFailed(payment, params);
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }

        if (isConfiguredForRealVnPay() && !hasExpectedAmount(payment, params)) {
            markFailed(payment, params);
            return Map.of("RspCode", "04", "Message", "Invalid payment amount");
        }

        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "");
        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            markPaid(payment, params);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        }

        markFailed(payment, params);
        return Map.of("RspCode", "00", "Message", "Payment failed");
    }

    private boolean hasExpectedAmount(Payment payment, Map<String, String> params) {
        return toVnPayAmount(payment.getAmount()).equals(params.get("vnp_Amount"));
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

    private String buildPaymentUrl(Payment payment, String orderInfo, String clientIp) {
        if (!isConfiguredForRealVnPay()) {
            String encodedRef = URLEncoder.encode(payment.getTransactionRef(), StandardCharsets.UTF_8);
            return returnUrl + "?vnp_TxnRef=" + encodedRef + "&vnp_ResponseCode=00&devMode=true";
        }

        Instant now = Instant.now();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", toVnPayAmount(payment.getAmount()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getTransactionRef());
        params.put("vnp_OrderInfo", orderInfo == null || orderInfo.isBlank() ? "GameTrust payment" : orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", clientIp == null ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", VNPAY_TIME_FORMAT.format(now));
        params.put("vnp_ExpireDate", VNPAY_TIME_FORMAT.format(now.plusSeconds(900)));

        String hashData = VnPayUtil.buildHashData(params);
        String secureHash = VnPayUtil.hmacSha512(hashSecret, hashData);
        String query = VnPayUtil.buildQuery(params);
        return payUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    private boolean verifySignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }
        Map<String, String> signedParams = new TreeMap<>(params);
        signedParams.remove("vnp_SecureHash");
        signedParams.remove("vnp_SecureHashType");
        String hashData = VnPayUtil.buildHashData(signedParams);
        return receivedHash.equalsIgnoreCase(VnPayUtil.hmacSha512(hashSecret, hashData));
    }

    private boolean isConfiguredForRealVnPay() {
        return tmnCode != null && !tmnCode.isBlank() && hashSecret != null && !hashSecret.isBlank();
    }

    private String toVnPayAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
