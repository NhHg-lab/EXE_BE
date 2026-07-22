package com.teaverse.compensation.dto.request;

import com.teaverse.compensation.model.PostType;
import jakarta.validation.constraints.NotBlank;

public record CreatePostRequest(
        PostType type,
        @NotBlank String content,
        String game,
        String clanTag
) {
}
