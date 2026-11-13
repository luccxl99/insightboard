package com.insightboard.service;

import com.insightboard.domain.Role;
import com.insightboard.domain.Tenant;
import com.insightboard.domain.User;
import com.insightboard.repository.TenantRepository;
import com.insightboard.repository.UserRepository;
import com.insightboard.security.AuthUser;
import com.insightboard.security.JwtService;
import com.insightboard.web.ApiException;
import com.insightboard.web.dto.Dtos.AuthResponse;
import com.insightboard.web.dto.Dtos.LoginRequest;
import com.insightboard.web.dto.Dtos.MeResponse;
import com.insightboard.web.dto.Dtos.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final TenantRepository tenants;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;

    /** Cadastro self-service: cria um novo tenant e o primeiro usuario (ADMIN) dele. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Já existe uma conta com este e-mail");
        }
        Tenant tenant = tenants.save(new Tenant(request.companyName().trim()));

        User user = new User();
        user.setTenantId(tenant.getId());
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setRole(Role.ADMIN);
        users.save(user);

        audit.log(tenant.getId(), email, "TENANT_REGISTERED", "Tenant", tenant.getId().toString(), tenant.getName());
        return new AuthResponse(jwt.generate(user), toMe(user, tenant.getName()));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(u -> encoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("E-mail ou senha inválidos"));
        Tenant tenant = tenants.findById(user.getTenantId()).orElseThrow();

        audit.log(user.getTenantId(), user.getEmail(), "LOGIN", "User", user.getId().toString(), null);
        return new AuthResponse(jwt.generate(user), toMe(user, tenant.getName()));
    }

    @Transactional(readOnly = true)
    public MeResponse me(AuthUser authUser) {
        User user = users.findById(authUser.userId())
                .orElseThrow(() -> ApiException.unauthorized("Usuário não encontrado"));
        Tenant tenant = tenants.findById(user.getTenantId()).orElseThrow();
        return toMe(user, tenant.getName());
    }

    private MeResponse toMe(User user, String tenantName) {
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), tenantName);
    }
}
