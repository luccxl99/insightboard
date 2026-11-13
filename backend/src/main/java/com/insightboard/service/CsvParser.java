package com.insightboard.service;

import com.insightboard.web.ApiException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CsvParser {

    public static final int MAX_ROWS = 50_000;
    public static final int MAX_COLUMNS = 100;

    public record ParsedCsv(List<String> headers, List<Map<String, String>> rows) {
    }

    public ParsedCsv parse(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) {
            text = text.substring(1); // BOM gerado pelo Excel
        }
        if (text.isBlank()) {
            throw ApiException.badRequest("O arquivo CSV está vazio");
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(detectDelimiter(text))
                .setHeader()
                .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW) // o padrao aceita e a 2a coluna sobrescreveria a 1a
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = format.parse(new StringReader(text))) {
            List<String> headers = parser.getHeaderNames();
            if (headers.size() > MAX_COLUMNS) {
                throw ApiException.badRequest("O CSV excede o limite de " + MAX_COLUMNS + " colunas");
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (rows.size() >= MAX_ROWS) {
                    throw ApiException.badRequest("O CSV excede o limite de " + MAX_ROWS + " linhas");
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (String header : headers) {
                    row.put(header, record.isSet(header) ? record.get(header) : "");
                }
                rows.add(row);
            }
            if (rows.isEmpty()) {
                throw ApiException.badRequest("O CSV não possui linhas de dados");
            }
            return new ParsedCsv(headers, rows);
        } catch (ApiException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw ApiException.badRequest(
                    "CSV inválido: verifique se o cabeçalho não tem colunas vazias ou repetidas");
        }
    }

    /** Excel em pt-BR exporta com ';', entao detectamos entre ',', ';' e tab pela primeira linha. */
    static char detectDelimiter(String text) {
        int end = text.indexOf('\n');
        String firstLine = end < 0 ? text : text.substring(0, end);
        int commas = count(firstLine, ',');
        int semicolons = count(firstLine, ';');
        int tabs = count(firstLine, '\t');
        if (semicolons > commas && semicolons >= tabs) {
            return ';';
        }
        if (tabs > commas) {
            return '\t';
        }
        return ',';
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }
}
