package com.teaverse.compensation.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoMoSignatureUtilTest {
    @Test
    void createsHmacSha256UsingUtf8() {
        String signature = MoMoSignatureUtil.hmacSha256(
                "key",
                "The quick brown fox jumps over the lazy dog"
        );

        assertThat(signature)
                .isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }

    @Test
    void createsPaymentSignatureDataInMoMoDocumentedOrder() {
        String rawData = MoMoSignatureUtil.createRequestData(
                "ACCESS",
                15000,
                "",
                "https://api.example.com/ipn",
                "GT-123",
                "Tournament entry",
                "PARTNER",
                "https://example.com/momo-result",
                "REQ-123",
                "captureWallet"
        );

        assertThat(rawData).isEqualTo(
                "accessKey=ACCESS&amount=15000&extraData=&ipnUrl=https://api.example.com/ipn"
                        + "&orderId=GT-123&orderInfo=Tournament entry&partnerCode=PARTNER"
                        + "&redirectUrl=https://example.com/momo-result&requestId=REQ-123"
                        + "&requestType=captureWallet"
        );
    }
}
