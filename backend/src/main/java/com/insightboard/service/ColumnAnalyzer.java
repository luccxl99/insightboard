package com.insightboard.service;

import com.insightboard.domain.ColumnInfo;
import com.insightboard.domain.ColumnType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Infere o tipo de cada coluna: NUMBER quando ao menos 95% dos valores preenchidos sao numericos. */
@Component
public class ColumnAnalyzer {

    private static final double NUMERIC_THRESHOLD = 0.95;

    public List<ColumnInfo> infer(List<String> headers, List<Map<String, String>> rows) {
        List<ColumnInfo> result = new ArrayList<>(headers.size());
        for (String header : headers) {
            long filled = 0;
            long numeric = 0;
            for (Map<String, String> row : rows) {
                String value = row.get(header);
                if (value == null || value.isBlank()) {
                    continue;
                }
                filled++;
                if (NumberParser.parse(value) != null) {
                    numeric++;
                }
            }
            boolean isNumber = filled > 0 && (double) numeric / filled >= NUMERIC_THRESHOLD;
            result.add(new ColumnInfo(header, isNumber ? ColumnType.NUMBER : ColumnType.TEXT));
        }
        return result;
    }
}
