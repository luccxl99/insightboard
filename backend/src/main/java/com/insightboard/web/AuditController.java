package com.insightboard.web;

import com.insightboard.security.AuthUser;
import com.insightboard.service.AuditService;
import com.insightboard.web.dto.Dtos.AuditLogResponse;
import com.insightboard.web.dto.Dtos.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public PageResponse<AuditLogResponse> list(@AuthenticationPrincipal AuthUser user,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return auditService.list(user, Math.max(0, page), Math.min(Math.max(1, size), 100));
    }
}
