package com.scholarship.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Turns table data into CSV text and saves it. Used by the Reports tab. */
public class CsvExporter {

    /** Makes one value safe for CSV: quotes commas/quotes/newlines and blocks Excel formulas. */
    public static String escape(String value) {
        if (value == null) return "";
        String v = value;
        // A cell starting with = + - @ can run as a formula in Excel, so add a ' in front (plain numbers are fine)
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0 && !v.matches("-?\\d+(\\.\\d+)?")) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    public static String toCsv(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, headers);
        for (String[] row : rows) appendRow(sb, row);
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, String[] cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cells[i]));
        }
        sb.append("\r\n");
    }

    /** Saves as UTF-8 with a BOM so Excel shows special characters correctly. */
    public static void write(Path file, String[] headers, List<String[]> rows) throws IOException {
        Files.writeString(file, "\uFEFF" + toCsv(headers, rows), StandardCharsets.UTF_8);
    }
}