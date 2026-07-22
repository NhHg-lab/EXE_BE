package com.teaverse.compensation.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateTeamRequest(
        @NotBlank String name,
        List<String> memberIds
) {
}
