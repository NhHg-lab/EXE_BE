package com.teaverse.compensation.dto.response;

import com.teaverse.compensation.model.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        String id,
        NotificationType type,
        String title,
        String content,
        boolean unread,
        Instant createdAt
) {
}
