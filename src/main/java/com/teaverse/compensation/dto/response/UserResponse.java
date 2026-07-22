package com.teaverse.compensation.dto.response;

import com.teaverse.compensation.model.GameProfile;
import com.teaverse.compensation.model.UserRole;
import java.time.Instant;

public record UserResponse(
        String id,
        String email,
        String username,
        String fullName,
        UserRole role,
        boolean premium,
        boolean verified,
        double trustScore,
        GameProfile gameProfile,
        Instant createdAt
) {
}
