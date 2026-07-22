package com.teaverse.compensation.dto.request;

import com.teaverse.compensation.model.GameProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank String fullName,
        @Valid GameProfile gameProfile
) {
}
