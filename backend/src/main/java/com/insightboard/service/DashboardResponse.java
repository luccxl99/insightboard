package com.insightboard.service;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/** Serializable porque o resultado e cacheado no Redis. */
public record DashboardResponse(UUID datasetId, String name, int rowCount, int columnCount,
                                List<NumericSummary> numeric,
                                List<CategorySummary> categorical) implements Serializable {

    public record Bin(String label, long count) implements Serializable {
    }

    public record NumericSummary(String column, long count, double sum, double min, double max, double avg,
                                 List<Bin> histogram) implements Serializable {
    }

    public record ValueCount(String value, long count) implements Serializable {
    }

    public record CategorySummary(String column, int distinct, long filled,
                                  List<ValueCount> top) implements Serializable {
    }
}
