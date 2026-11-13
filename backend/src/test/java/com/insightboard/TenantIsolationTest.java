package com.insightboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao ponta a ponta com PostgreSQL real (Testcontainers).
 * O cache fica desligado aqui; o foco e autenticacao, permissoes e isolamento entre tenants.
 */
@SpringBootTest(properties = "spring.cache.type=none")
@AutoConfigureMockMvc
@Testcontainers
class TenantIsolationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private static final String CSV = "regiao,valor\nSul,10\nNorte,20\nSul,30\n";

    private record Session(String token) {
    }

    private Session register(String company, String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "companyName", company, "name", "Admin", "email", email, "password", "senha-forte-123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return new Session(json.readTree(r.getResponse().getContentAsString()).get("token").asText());
    }

    private String uploadAs(Session s) throws Exception {
        MvcResult r = mvc.perform(multipart("/api/datasets")
                        .file(new MockMultipartFile("file", "vendas.csv", "text/csv", CSV.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void tenantsCannotSeeEachOthersData() throws Exception {
        Session a = register("Empresa A", "admin-a-" + UUID.randomUUID() + "@a.com");
        Session b = register("Empresa B", "admin-b-" + UUID.randomUUID() + "@b.com");
        String datasetOfA = uploadAs(a);

        // dono acessa normalmente
        mvc.perform(get("/api/datasets/" + datasetOfA + "/dashboard").header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowCount").value(3))
                .andExpect(jsonPath("$.numeric[0].sum").value(60.0));

        // outro tenant recebe 404 em todos os recursos do dataset (nao vaza nem a existencia)
        for (String path : new String[]{"", "/rows", "/dashboard", "/export/pdf"}) {
            mvc.perform(get("/api/datasets/" + datasetOfA + path).header("Authorization", "Bearer " + b.token()))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(delete("/api/datasets/" + datasetOfA).header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isNotFound());

        // a listagem do tenant B nao contem o dataset de A
        mvc.perform(get("/api/datasets").header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rolesAreEnforced() throws Exception {
        Session admin = register("Empresa C", "admin-c-" + UUID.randomUUID() + "@c.com");
        String datasetId = uploadAs(admin);

        String viewerEmail = "viewer-" + UUID.randomUUID() + "@c.com";
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "name", "Visitante", "email", viewerEmail, "password", "senha-forte-123", "role", "VIEWER"))))
                .andExpect(status().isCreated());

        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", viewerEmail, "password", "senha-forte-123"))))
                .andExpect(status().isOk()).andReturn();
        String viewer = "Bearer " + json.readTree(login.getResponse().getContentAsString()).get("token").asText();

        // viewer enxerga, mas nao altera
        mvc.perform(get("/api/datasets/" + datasetId + "/dashboard").header("Authorization", viewer))
                .andExpect(status().isOk());
        mvc.perform(multipart("/api/datasets")
                        .file(new MockMultipartFile("file", "x.csv", "text/csv", CSV.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", viewer))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/datasets/" + datasetId + "/export/pdf").header("Authorization", viewer))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/datasets/" + datasetId).header("Authorization", viewer))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").header("Authorization", viewer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/users").header("Authorization", viewer)).andExpect(status().isForbidden());

        // admin exporta PDF e ve a auditoria
        MvcResult pdf = mvc.perform(get("/api/datasets/" + datasetId + "/export/pdf")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk()).andReturn();
        assertThat(pdf.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 4, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF");

        MvcResult audit = mvc.perform(get("/api/audit").header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk()).andReturn();
        JsonNode actions = json.readTree(audit.getResponse().getContentAsString()).get("content");
        assertThat(actions.toString()).contains("DATASET_UPLOADED", "USER_CREATED", "DATASET_EXPORTED_PDF");
    }

    @Test
    void protectedEndpointsRequireValidToken() throws Exception {
        mvc.perform(get("/api/datasets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/datasets").header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejectedAndDuplicateEmailConflicts() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@d.com";
        register("Empresa D", email);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "errada-123"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "companyName", "Outra", "name", "X", "email", email, "password", "senha-forte-123"))))
                .andExpect(status().isConflict());
    }
}
