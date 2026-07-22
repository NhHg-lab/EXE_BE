package com.teaverse.compensation.dto.response;

public record MatchResponse(
        String userId,
        String username,
        String mainGame,
        String rank,
        String goal,
        double trustScore,
        int matchScore
) {
}
