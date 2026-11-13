package com.insightboard.web;

import com.insightboard.security.AuthUser;
import com.insightboard.service.DashboardResponse;
import com.insightboard.service.DashboardService;
import com.insightboard.service.DatasetService;
import com.insightboard.service.PdfExportService;
import com.insightboard.service.PdfExportService.PdfFile;
import com.insightboard.web.dto.Dtos.DatasetResponse;
import com.insightboard.web.dto.Dtos.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;
    private final DashboardService dashboardService;
    private final PdfExportService pdfExportService;

    @GetMapping
    public List<DatasetResponse> list(@AuthenticationPrincipal AuthUser user) {
        return datasetService.list(user);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public DatasetResponse upload(@AuthenticationPrincipal AuthUser user,
                                  @RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "name", required = false) String name) {
        return datasetService.upload(user, file, name);
    }

    @GetMapping("/{id}")
    public DatasetResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return datasetService.get(user, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        datasetService.delete(user, id);
    }

    @GetMapping("/{id}/rows")
    public PageResponse<Map<String, String>> rows(@AuthenticationPrincipal AuthUser user,
                                                  @PathVariable UUID id,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return datasetService.rows(user, id, Math.max(0, page), Math.min(Math.max(1, size), 100));
    }

    @GetMapping("/{id}/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return dashboardService.build(user.tenantId(), id);
    }

    @GetMapping("/{id}/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<byte[]> exportPdf(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        PdfFile pdf = pdfExportService.export(user, id);
        String filename = pdf.name().replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }
}
