package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.response.NotificationResponse;
import com.teaverse.compensation.exception.NotFoundException;
import com.teaverse.compensation.mapper.DtoMapper;
import com.teaverse.compensation.model.Notification;
import com.teaverse.compensation.model.NotificationType;
import com.teaverse.compensation.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final DtoMapper mapper;

    public NotificationService(
            NotificationRepository notificationRepository,
            CurrentUserService currentUserService,
            DtoMapper mapper
    ) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    public Notification create(String userId, NotificationType type, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        return notificationRepository.save(notification);
    }

    public List<NotificationResponse> myNotifications() {
        String userId = currentUserService.getCurrentUser().getId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::toNotificationResponse)
                .toList();
    }

    public void markRead(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        ensureOwner(notification);
        notification.setUnread(false);
        notificationRepository.save(notification);
    }

    public void dismiss(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        ensureOwner(notification);
        notificationRepository.delete(notification);
    }

    public void markAllRead() {
        String userId = currentUserService.getCurrentUser().getId();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setUnread(false));
        notificationRepository.saveAll(notifications);
    }

    public void clearAll() {
        String userId = currentUserService.getCurrentUser().getId();
        notificationRepository.deleteAll(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    private void ensureOwner(Notification notification) {
        String userId = currentUserService.getCurrentUser().getId();
        if (!notification.getUserId().equals(userId)) {
            throw new NotFoundException("Notification not found");
        }
    }
}
