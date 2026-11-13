package com.insightboard.service;

import com.insightboard.domain.ColumnInfo;
import com.insightboard.domain.Dataset;
import com.insightboard.domain.DatasetRow;
import com.insightboard.repository.DatasetRepository;
import com.insightboard.repository.DatasetRowRepository;
import com.insightboard.security.AuthUser;
import com.insightboard.service.CsvParser.ParsedCsv;
import com.insightboard.web.ApiException;
import com.insightboard.web.dto.Dtos.DatasetResponse;
import com.insightboard.web.dto.Dtos.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DatasetService {

    private static final int INSERT_BATCH = 1000;

    private final DatasetRepository datasets;
    private final DatasetRowRepository rows;
    private final CsvParser csvParser;
    private final ColumnAnalyzer columnAnalyzer;
    private final DashboardService dashboard;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<DatasetResponse> list(AuthUser user) {
        return datasets.findByTenantIdOrderByCreatedAtDesc(user.tenantId()).stream()
                .map(DatasetResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DatasetResponse get(AuthUser user, UUID id) {
        return DatasetResponse.from(findOwned(user, id));
    }

    @Transactional
    public DatasetResponse upload(AuthUser user, MultipartFile file, String name) {
        String filename = file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename();
        if (file.isEmpty() || !filename.toLowerCase().endsWith(".csv")) {
            throw ApiException.badRequest("Envie um arquivo .csv válido");
        }
        ParsedCsv csv;
        try {
            csv = csvParser.parse(file.getBytes());
        } catch (IOException e) {
            throw ApiException.badRequest("Não foi possível ler o arquivo enviado");
        }
        List<ColumnInfo> columns = columnAnalyzer.infer(csv.headers(), csv.rows());

        Dataset dataset = new Dataset();
        dataset.setTenantId(user.tenantId());
        dataset.setName(resolveName(name, filename));
        dataset.setOriginalFilename(truncate(filename, 255));
        dataset.setRowCount(csv.rows().size());
        dataset.setColumns(columns);
        dataset.setCreatedBy(user.email());
        datasets.save(dataset);

        List<DatasetRow> batch = new ArrayList<>(INSERT_BATCH);
        int line = 1;
        for (Map<String, String> data : csv.rows()) {
            DatasetRow row = new DatasetRow();
            row.setDatasetId(dataset.getId());
            row.setTenantId(user.tenantId());
            row.setLineNumber(line++);
            row.setData(data);
            batch.add(row);
            if (batch.size() == INSERT_BATCH) {
                rows.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            rows.saveAll(batch);
        }

        audit.log(user, "DATASET_UPLOADED", "Dataset", dataset.getId().toString(),
                dataset.getName() + " (" + dataset.getRowCount() + " linhas)");
        return DatasetResponse.from(dataset);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, String>> rows(AuthUser user, UUID id, int page, int size) {
        Dataset dataset = findOwned(user, id);
        Page<DatasetRow> result = rows.findByDatasetIdAndTenantIdOrderByLineNumber(
                dataset.getId(), user.tenantId(), PageRequest.of(page, size));
        return new PageResponse<>(result.map(DatasetRow::getData).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public void delete(AuthUser user, UUID id) {
        Dataset dataset = findOwned(user, id);
        datasets.delete(dataset); // as linhas saem junto via ON DELETE CASCADE
        dashboard.evict(user.tenantId(), id);
        audit.log(user, "DATASET_DELETED", "Dataset", id.toString(), dataset.getName());
    }

    /** Ponto unico de acesso a um dataset: filtra por tenant, entao id de outro tenant resulta em 404. */
    Dataset findOwned(AuthUser user, UUID id) {
        return datasets.findByIdAndTenantId(id, user.tenantId())
                .orElseThrow(() -> ApiException.notFound("Dataset não encontrado"));
    }

    private static String resolveName(String requested, String filename) {
        String base = requested != null && !requested.isBlank()
                ? requested.trim()
                : filename.replaceFirst("(?i)\\.csv$", "");
        return truncate(base, 150);
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }
}
