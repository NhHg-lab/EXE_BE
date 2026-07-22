package com.teaverse.compensation.dto.response;

import com.teaverse.compensation.model.RegistrationStatus;

public record RegistrationResponse(
        String id,
        String tournamentId,
        String userId,
        String teamId,
        RegistrationStatus status,
        double smurfScore,
        String smurfRiskLevel,
        String paymentId,
        String paymentUrl
) {
}
