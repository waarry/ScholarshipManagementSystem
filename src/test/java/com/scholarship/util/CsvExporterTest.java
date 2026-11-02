package com.scholarship.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvExporterTest {

    @Test
    void plainTextIsUnchanged() {
        assertEquals("hello", CsvExporter.escape("hello"));
    }

    @Test
    void nullBecomesEmpty() {
        assertEquals("", CsvExporter.escape(null));
    }

    @Test
    void commaGetsQuoted() {
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"));
    }

    @Test
    void quotesAreDoubled() {
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""));
    }

    @Test
    void formulaIsNeutralised() {
        assertEquals("'=1+1", CsvExporter.escape("=1+1"));
    }

    @Test
    void plainNegativeNumberIsKept() {
        assertEquals("-5", CsvExporter.escape("-5"));
    }

    @Test
    void wholeCsvIsBuiltCorrectly() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"1", "x,y"});
        String csv = CsvExporter.toCsv(new String[]{"A", "B"}, rows);
        assertEquals("A,B\r\n1,\"x,y\"\r\n", csv);
    }
}