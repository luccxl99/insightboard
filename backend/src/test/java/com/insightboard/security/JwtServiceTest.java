package com.insightboard.security;

import com.insightboard.domain.Role;
import com.insightboard.domain.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-long!!";

    private User user() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setTenantId(UUID.randomUUID());
        u.setEmail("ana@empresa.com");
        u.setRole(Role.ANALYST);
        return u;
    }

    @Test
    void roundTripPreservesIdentityTenantAndRole() {
        JwtService jwt = new JwtService(SECRET, 60);
        User u = user();

        AuthUser parsed = jwt.parse(jwt.generate(u)).orElseThrow();

        assertThat(parsed.userId()).isEqualTo(u.getId());
        assertThat(parsed.tenantId()).isEqualTo(u.getTenantId());
        assertThat(parsed.email()).isEqualTo("ana@empresa.com");
        assertThat(parsed.role()).isEqualTo(Role.ANALYST);
    }

    @Test
    void rejectsTamperedExpiredAndForeignTokens() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.generate(user());

        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThat(jwt.parse(tampered)).isEmpty();
        assertThat(jwt.parse("not-a-jwt")).isEmpty();

        JwtService expired = new JwtService(SECRET, -1);
        assertThat(jwt.parse(expired.generate(user()))).isEmpty();

        JwtService otherKey = new JwtService("another-secret-with-at-least-32-bytes!!", 60);
        assertThat(jwt.parse(otherKey.generate(user()))).isEmpty();
    }

    @Test
    void refusesWeakSecret() {
        assertThatThrownBy(() -> new JwtService("short", 60)).isInstanceOf(IllegalStateException.class);
    }
}
