package com.insightboard.repository;

import com.insightboard.domain.DatasetRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DatasetRowRepository extends JpaRepository<DatasetRow, UUID> {
    Page<DatasetRow> findByDatasetIdAndTenantIdOrderByLineNumber(UUID datasetId, UUID tenantId, Pageable pageable);

    List<DatasetRow> findByDatasetIdAndTenantId(UUID datasetId, UUID tenantId);
}
