package com.teaverse.compensation.dto.request;

import com.teaverse.compensation.model.GameProfile;
import com.teaverse.compensation.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank String username,
        @NotBlank String fullName,
        @NotNull UserRole role,
        GameProfile gameProfile
) {
}
