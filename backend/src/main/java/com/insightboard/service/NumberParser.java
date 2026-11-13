package com.insightboard.service;

import java.util.regex.Pattern;

/**
 * Converte textos numericos de CSVs reais ("1234.56", "1.234,56", "R$ 1,234.56", "12%") em double.
 * Retorna null quando o texto nao e um numero.
 */
public final class NumberParser {

    private static final Pattern PLAIN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Pattern DOT_THOUSANDS = Pattern.compile("^-?\\d{1,3}(\\.\\d{3})+$");
    private static final Pattern COMMA_THOUSANDS = Pattern.compile("^-?\\d{1,3}(,\\d{3})+$");

    private NumberParser() {
    }

    public static Double parse(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim()
                .replace("R$", "").replace("$", "").replace("%", "")
                .replace(" ", "").replace(" ", "");
        if (s.isEmpty()) {
            return null;
        }

        int lastComma = s.lastIndexOf(',');
        int lastDot = s.lastIndexOf('.');

        if (lastComma >= 0 && lastDot >= 0) {
            // O ultimo separador e o decimal: "1.234,56" (pt-BR) ou "1,234.56" (en-US)
            s = lastComma > lastDot
                    ? s.replace(".", "").replace(',', '.')
                    : s.replace(",", "");
        } else if (lastComma >= 0) {
            if (COMMA_THOUSANDS.matcher(s).matches() && s.indexOf(',') != s.lastIndexOf(',')) {
                s = s.replace(",", "");
            } else if (s.indexOf(',') == lastComma) {
                s = s.replace(',', '.'); // "12,5" -> decimal brasileiro
            } else {
                return null;
            }
        } else if (lastDot >= 0 && s.indexOf('.') != lastDot) {
            // varios pontos so valem como separador de milhar ("1.234.567"); evita tratar "15.01.2024" como numero
            if (!DOT_THOUSANDS.matcher(s).matches()) {
                return null;
            }
            s = s.replace(".", "");
        }

        if (!PLAIN.matcher(s).matches()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
