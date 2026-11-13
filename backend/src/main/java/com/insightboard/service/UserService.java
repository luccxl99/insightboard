package com.insightboard.service;

import com.insightboard.domain.User;
import com.insightboard.repository.UserRepository;
import com.insightboard.security.AuthUser;
import com.insightboard.web.ApiException;
import com.insightboard.web.dto.Dtos.CreateUserRequest;
import com.insightboard.web.dto.Dtos.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<UserResponse> list(AuthUser actor) {
        return users.findByTenantIdOrderByCreatedAtAsc(actor.tenantId()).stream()
                .map(UserResponse::from)
                .toList();
    }

    /** O novo usuario sempre nasce no tenant de quem o criou: o tenant nunca vem do request. */
    @Transactional
    public UserResponse create(AuthUser actor, CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Já existe uma conta com este e-mail");
        }
        User user = new User();
        user.setTenantId(actor.tenantId());
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setRole(request.role());
        users.save(user);

        audit.log(actor, "USER_CREATED", "User", user.getId().toString(), email + " (" + user.getRole() + ")");
        return UserResponse.from(user);
    }
}
