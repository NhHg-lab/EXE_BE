package com.teaverse.compensation.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class VnPayUtilTest {

    @Test
    void hmacSha512MatchesKnownTestVector() {
        String hash = VnPayUtil.hmacSha512("Jefe", "what do ya want for nothing?");

        assertThat(hash).isEqualTo(
                "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea250554"
                        + "9758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737"
        );
    }

    @Test
    void hashDataSortsKeysAndUrlEncodesUtf8Values() {
        Map<String, String> params = Map.of(
                "b", "hello world",
                "a", "a"
        );

        assertThat(VnPayUtil.buildHashData(params)).isEqualTo("a=a&b=hello+world");
        assertThat(VnPayUtil.buildQuery(params)).isEqualTo("a=a&b=hello+world");
    }
}

