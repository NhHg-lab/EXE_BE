package com.teaverse.compensation.controller;

import com.teaverse.compensation.dto.response.ApiResponse;
import com.teaverse.compensation.dto.response.NotificationResponse;
import com.teaverse.compensation.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list() {
        return ApiResponse.ok(notificationService.myNotifications());
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable String id) {
        notificationService.markRead(id);
        return ApiResponse.ok("Notification marked as read", null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.ok("All notifications marked as read", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> dismiss(@PathVariable String id) {
        notificationService.dismiss(id);
        return ApiResponse.ok("Notification dismissed", null);
    }

    @DeleteMapping
    public ApiResponse<Void> clearAll() {
        notificationService.clearAll();
        return ApiResponse.ok("Notifications cleared", null);
    }
}
