package com.teaverse.compensation.dto.response;

import com.teaverse.compensation.model.PostType;
import java.time.Instant;
import java.util.List;

public record PostResponse(
        String id,
        String authorId,
        String authorName,
        PostType type,
        String content,
        String game,
        String clanTag,
        int likes,
        List<String> comments,
        boolean sponsored,
        Instant createdAt
) {
}
