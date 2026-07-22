package com.teaverse.compensation.dto.response;

import java.util.List;

public record TeamResponse(
        String id,
        String tournamentId,
        String name,
        String captainId,
        List<String> memberIds
) {
}
