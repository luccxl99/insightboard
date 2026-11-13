package com.insightboard.domain;

import java.io.Serializable;

public record ColumnInfo(String name, ColumnType type) implements Serializable {
}
