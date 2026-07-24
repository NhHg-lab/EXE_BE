package com.teaverse.compensation.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class MoMoSignatureUtil {
    private MoMoSignatureUtil() {
    }

    public static String hmacSha256(String secretKey, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to create MoMo signature", ex);
        }
    }

    public static boolean matches(String expected, String received) {
        if (expected == null || received == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase().getBytes(StandardCharsets.US_ASCII)
        );
    }

    public static String createRequestData(
            String accessKey,
            long amount,
            String extraData,
            String ipnUrl,
            String orderId,
            String orderInfo,
            String partnerCode,
            String redirectUrl,
            String requestId,
            String requestType
    ) {
        return "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
    }

    public static String createResponseData(
            String accessKey,
            long amount,
            String message,
            String orderId,
            String partnerCode,
            String payUrl,
            String requestId,
            long responseTime,
            int resultCode
    ) {
        return "accessKey=" + accessKey
                + "&amount=" + amount
                + "&message=" + message
                + "&orderId=" + orderId
                + "&partnerCode=" + partnerCode
                + "&payUrl=" + payUrl
                + "&requestId=" + requestId
                + "&responseTime=" + responseTime
                + "&resultCode=" + resultCode;
    }

    public static String paymentResultData(
            String accessKey,
            long amount,
            String extraData,
            String message,
            String orderId,
            String orderInfo,
            String orderType,
            String partnerCode,
            String payType,
            String requestId,
            long responseTime,
            int resultCode,
            long transId
    ) {
        return "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&message=" + message
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&orderType=" + orderType
                + "&partnerCode=" + partnerCode
                + "&payType=" + payType
                + "&requestId=" + requestId
                + "&responseTime=" + responseTime
                + "&resultCode=" + resultCode
                + "&transId=" + transId;
    }
}
