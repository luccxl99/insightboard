package com.insightboard.service;

import com.insightboard.domain.AuditLog;
import com.insightboard.repository.AuditLogRepository;
import com.insightboard.security.AuthUser;
import com.insightboard.web.dto.Dtos.AuditLogResponse;
import com.insightboard.web.dto.Dtos.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    public void log(UUID tenantId, String actor, String action, String entity, String entityId, String detail) {
        AuditLog entry = new AuditLog();
        entry.setTenantId(tenantId);
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntity(entity);
        entry.setEntityId(entityId);
        entry.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
        repository.save(entry);
    }

    public void log(AuthUser user, String action, String entity, String entityId, String detail) {
        log(user.tenantId(), user.email(), action, entity, entityId, detail);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(AuthUser user, int page, int size) {
        Page<AuditLog> result = repository.findByTenantIdOrderByCreatedAtDesc(
                user.tenantId(), PageRequest.of(page, size));
        return new PageResponse<>(result.map(AuditLogResponse::from).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
