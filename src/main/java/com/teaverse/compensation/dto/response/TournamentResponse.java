package com.teaverse.compensation.dto.response;

import com.teaverse.compensation.model.BracketMatch;
import com.teaverse.compensation.model.TournamentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TournamentResponse(
        String id,
        String title,
        String game,
        TournamentStatus status,
        String mode,
        String format,
        int teamSize,
        int maxTeams,
        int registeredTeams,
        BigDecimal entryFee,
        BigDecimal prizePool,
        Instant startsAt,
        String organizerId,
        String description,
        List<BracketMatch> bracket
) {
}
