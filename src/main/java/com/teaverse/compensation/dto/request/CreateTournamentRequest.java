package com.teaverse.compensation.dto.request;

import com.teaverse.compensation.model.TournamentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateTournamentRequest(
        @NotBlank String title,
        @NotBlank String game,
        TournamentStatus status,
        String mode,
        String format,
        @Min(1) int teamSize,
        @Min(2) int maxTeams,
        @DecimalMin("0.0") BigDecimal entryFee,
        @DecimalMin("0.0") BigDecimal prizePool,
        Instant startsAt,
        String description
) {
}
