package com.insightboard.web.dto;

import com.insightboard.domain.AuditLog;
import com.insightboard.domain.ColumnInfo;
import com.insightboard.domain.Dataset;
import com.insightboard.domain.Role;
import com.insightboard.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Dtos {

    private Dtos() {
    }

    // ---- auth ----
    public record RegisterRequest(
            @NotBlank @Size(max = 120) String companyName,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String password) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record MeResponse(UUID id, String name, String email, Role role, String tenantName) {
    }

    public record AuthResponse(String token, MeResponse me) {
    }

    // ---- users ----
    public record CreateUserRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String password,
            @NotNull Role role) {
    }

    public record UserResponse(UUID id, String name, String email, Role role, Instant createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt());
        }
    }

    // ---- datasets ----
    public record DatasetResponse(UUID id, String name, String originalFilename, int rowCount,
                                  List<ColumnInfo> columns, String createdBy, Instant createdAt) {
        public static DatasetResponse from(Dataset d) {
            return new DatasetResponse(d.getId(), d.getName(), d.getOriginalFilename(), d.getRowCount(),
                    d.getColumns(), d.getCreatedBy(), d.getCreatedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }

    // ---- audit ----
    public record AuditLogResponse(UUID id, String actor, String action, String entity, String entityId,
                                   String detail, Instant createdAt) {
        public static AuditLogResponse from(AuditLog a) {
            return new AuditLogResponse(a.getId(), a.getActor(), a.getAction(), a.getEntity(),
                    a.getEntityId(), a.getDetail(), a.getCreatedAt());
        }
    }
}
