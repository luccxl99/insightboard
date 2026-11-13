package com.insightboard.service;

import com.insightboard.domain.ColumnType;
import com.insightboard.service.CsvParser.ParsedCsv;
import com.insightboard.web.ApiException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvParserTest {

    private final CsvParser parser = new CsvParser();
    private final ColumnAnalyzer analyzer = new ColumnAnalyzer();

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void parsesCommaSeparatedCsv() {
        ParsedCsv csv = parser.parse(bytes("regiao,valor\nSul,10\nNorte,20\n"));

        assertThat(csv.headers()).containsExactly("regiao", "valor");
        assertThat(csv.rows()).hasSize(2);
        assertThat(csv.rows().get(1)).containsEntry("regiao", "Norte").containsEntry("valor", "20");
    }

    @Test
    void detectsSemicolonDelimiterAndStripsBom() {
        ParsedCsv csv = parser.parse(bytes("﻿regiao;valor\nSul;1.234,50\n"));

        assertThat(csv.headers()).containsExactly("regiao", "valor");
        assertThat(csv.rows().get(0)).containsEntry("valor", "1.234,50");
    }

    @Test
    void shortRowsAreFilledWithEmptyStrings() {
        ParsedCsv csv = parser.parse(bytes("a,b,c\n1,2\n"));

        assertThat(csv.rows().get(0)).containsEntry("c", "");
    }

    @Test
    void rejectsEmptyFileHeaderOnlyAndDuplicateHeaders() {
        assertThatThrownBy(() -> parser.parse(bytes("   ")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> parser.parse(bytes("a,b\n")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> parser.parse(bytes("a,a\n1,2\n")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void infersColumnTypes() {
        ParsedCsv csv = parser.parse(bytes("""
                produto,quantidade,valor
                caneta,10,"1.234,50"
                lapis,5,"99,90"
                borracha,,"3,00"
                """));

        var columns = analyzer.infer(csv.headers(), csv.rows());

        assertThat(columns).extracting("name", "type").containsExactly(
                org.assertj.core.groups.Tuple.tuple("produto", ColumnType.TEXT),
                org.assertj.core.groups.Tuple.tuple("quantidade", ColumnType.NUMBER),
                org.assertj.core.groups.Tuple.tuple("valor", ColumnType.NUMBER));
    }
}
