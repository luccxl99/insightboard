package com.insightboard.repository;

import com.insightboard.domain.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetRepository extends JpaRepository<Dataset, UUID> {
    List<Dataset> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /** Toda busca por id passa por aqui: o tenant_id faz parte da consulta. */
    Optional<Dataset> findByIdAndTenantId(UUID id, UUID tenantId);
}
