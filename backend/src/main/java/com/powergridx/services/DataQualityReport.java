package com.powergridx.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Describes what was read from one CSV file and what had to be left out (and why). */
public class DataQualityReport {

    /** A continuous run of rows that could not be used (e.g. a sensor outage). */
    public static class Gap {
        public final LocalDateTime start;
        public LocalDateTime end;
        public int rows;

        Gap(LocalDateTime start) {
            this.start = start;
            this.end = start;
        }
    }

    public final String dataset;
    public final String file;
    public final int intervalMinutes;
    public int totalRows;
    public int loadedRows;
    public int missingValueRows;
    public int malformedRows;
    public LocalDateTime firstTimestamp;
    public LocalDateTime lastTimestamp;
    public final List<Gap> gaps = new ArrayList<>();

    public DataQualityReport(String dataset, String file, int intervalMinutes) {
        this.dataset = dataset;
        this.file = file;
        this.intervalMinutes = intervalMinutes;
    }

    public int skippedRows() {
        return missingValueRows + malformedRows;
    }

    public double completenessPct() {
        return totalRows == 0 ? 0 : Math.round(10000.0 * loadedRows / totalRows) / 100.0;
    }

    public Map<String, Object> toMap(int maxGaps) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dataset", dataset);
        m.put("file", file);
        m.put("intervalMinutes", intervalMinutes);
        m.put("totalRows", totalRows);
        m.put("loadedRows", loadedRows);
        m.put("skippedRows", skippedRows());
        m.put("missingValueRows", missingValueRows);
        m.put("malformedRows", malformedRows);
        m.put("completenessPct", completenessPct());
        m.put("from", firstTimestamp == null ? null : firstTimestamp.toString());
        m.put("to", lastTimestamp == null ? null : lastTimestamp.toString());
        m.put("gapCount", gaps.size());

        List<Gap> longest = new ArrayList<>(gaps);
        longest.sort((a, b) -> Integer.compare(b.rows, a.rows));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Gap g : longest.subList(0, Math.min(maxGaps, longest.size()))) {
            Map<String, Object> gm = new LinkedHashMap<>();
            gm.put("from", g.start.toString());
            gm.put("to", g.end.toString());
            gm.put("rows", g.rows);
            gm.put("minutes", Duration.between(g.start, g.end).toMinutes() + intervalMinutes);
            out.add(gm);
        }
        m.put("longestGaps", out);
        return m;
    }
}
