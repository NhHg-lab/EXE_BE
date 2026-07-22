package com.teaverse.compensation.dto.response;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
