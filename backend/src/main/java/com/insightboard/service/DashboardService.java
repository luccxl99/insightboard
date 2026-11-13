package com.insightboard.service;

import com.insightboard.domain.ColumnInfo;
import com.insightboard.domain.ColumnType;
import com.insightboard.domain.Dataset;
import com.insightboard.domain.DatasetRow;
import com.insightboard.repository.DatasetRepository;
import com.insightboard.repository.DatasetRowRepository;
import com.insightboard.service.DashboardResponse.Bin;
import com.insightboard.service.DashboardResponse.CategorySummary;
import com.insightboard.service.DashboardResponse.NumericSummary;
import com.insightboard.service.DashboardResponse.ValueCount;
import com.insightboard.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int BINS = 10;
    private static final int TOP_VALUES = 10;

    private final DatasetRepository datasets;
    private final DatasetRowRepository rows;

    /**
     * Resultado cacheado no Redis por (tenant, dataset). A chave inclui o tenant para que
     * um dataset nunca seja servido a outro tenant, mesmo em caso de colisao de ids.
     */
    @Cacheable(cacheNames = "dashboard", key = "#tenantId + ':' + #datasetId")
    @Transactional(readOnly = true)
    public DashboardResponse build(UUID tenantId, UUID datasetId) {
        Dataset dataset = datasets.findByIdAndTenantId(datasetId, tenantId)
                .orElseThrow(() -> ApiException.notFound("Dataset não encontrado"));
        List<DatasetRow> data = rows.findByDatasetIdAndTenantId(datasetId, tenantId);

        List<NumericSummary> numeric = new ArrayList<>();
        List<CategorySummary> categorical = new ArrayList<>();

        for (ColumnInfo column : dataset.getColumns()) {
            if (column.type() == ColumnType.NUMBER) {
                summarizeNumeric(column.name(), data).ifPresent(numeric::add);
            } else {
                categorical.add(summarizeCategory(column.name(), data));
            }
        }
        return new DashboardResponse(dataset.getId(), dataset.getName(), dataset.getRowCount(),
                dataset.getColumns().size(), numeric, categorical);
    }

    @CacheEvict(cacheNames = "dashboard", key = "#tenantId + ':' + #datasetId")
    public void evict(UUID tenantId, UUID datasetId) {
        // a anotacao faz o trabalho
    }

    private java.util.Optional<NumericSummary> summarizeNumeric(String column, List<DatasetRow> data) {
        List<Double> values = new ArrayList<>();
        for (DatasetRow row : data) {
            Double v = NumberParser.parse(row.getData().get(column));
            if (v != null) {
                values.add(v);
            }
        }
        if (values.isEmpty()) {
            return java.util.Optional.empty();
        }
        DoubleSummaryStatistics stats = values.stream().mapToDouble(Double::doubleValue).summaryStatistics();
        return java.util.Optional.of(new NumericSummary(column, stats.getCount(), stats.getSum(),
                stats.getMin(), stats.getMax(), stats.getAverage(),
                histogram(values, stats.getMin(), stats.getMax())));
    }

    static List<Bin> histogram(List<Double> values, double min, double max) {
        if (min == max) {
            return List.of(new Bin(format(min), values.size()));
        }
        double width = (max - min) / BINS;
        long[] counts = new long[BINS];
        for (double v : values) {
            int idx = Math.min(BINS - 1, (int) ((v - min) / width));
            counts[idx]++;
        }
        List<Bin> bins = new ArrayList<>(BINS);
        for (int i = 0; i < BINS; i++) {
            bins.add(new Bin(format(min + i * width) + " – " + format(min + (i + 1) * width), counts[i]));
        }
        return bins;
    }

    private CategorySummary summarizeCategory(String column, List<DatasetRow> data) {
        Map<String, Long> counts = new HashMap<>();
        long filled = 0;
        for (DatasetRow row : data) {
            String v = row.getData().get(column);
            if (v == null || v.isBlank()) {
                continue;
            }
            filled++;
            counts.merge(v, 1L, Long::sum);
        }
        List<ValueCount> top = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
                .limit(TOP_VALUES)
                .map(e -> new ValueCount(e.getKey(), e.getValue()))
                .toList();
        return new CategorySummary(column, counts.size(), filled, top);
    }

    private static String format(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
