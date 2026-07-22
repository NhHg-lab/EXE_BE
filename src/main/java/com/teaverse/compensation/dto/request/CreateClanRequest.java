package com.teaverse.compensation.dto.request;

import com.teaverse.compensation.model.ClanStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateClanRequest(
        @NotBlank String name,
        @NotBlank String tag,
        String tier,
        String region,
        String description,
        List<String> games,
        String requirement,
        ClanStatus status
) {
}
