package com.teaverse.compensation.dto.request;

import java.util.List;

public record JoinTournamentRequest(
        String teamId,
        String teamName,
        List<String> memberIds
) {
}
