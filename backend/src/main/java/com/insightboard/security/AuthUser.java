package com.insightboard.security;

import com.insightboard.domain.Role;

import java.util.UUID;

/** Principal extraido do JWT. O tenantId daqui e a unica fonte de verdade para isolamento de dados. */
public record AuthUser(UUID userId, UUID tenantId, String email, Role role) {
}
