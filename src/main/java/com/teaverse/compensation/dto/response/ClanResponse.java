package com.teaverse.compensation.dto.response;

import com.teaverse.compensation.model.ClanStatus;
import java.util.List;

public record ClanResponse(
        String id,
        String name,
        String tag,
        String tier,
        String region,
        String description,
        List<String> games,
        String requirement,
        ClanStatus status,
        int wins,
        int rating,
        int members,
        boolean joined
) {
}
