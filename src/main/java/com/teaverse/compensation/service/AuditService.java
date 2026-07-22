package com.teaverse.compensation.service;

import com.teaverse.compensation.model.AuditLog;
import com.teaverse.compensation.repository.AuditLogRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String actorId, String action, String targetType, String targetId, Map<String, Object> metadata) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setMetadata(metadata);
        auditLogRepository.save(log);
    }
}
