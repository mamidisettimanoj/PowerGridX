package com.powergridx.services;

import com.powergridx.models.HouseholdRecord;
import com.powergridx.models.ZoneRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Streams the two source CSV files into typed records.
 *
 * Rows that cannot be used (missing sensor readings written as "?" or blanks, unparseable timestamps)
 * are skipped and recorded in a {@link DataQualityReport} instead of aborting the whole load.
 */
public class DataIngestionService {
    private static final Logger log = LoggerFactory.getLogger(DataIngestionService.class);

    public static final String HOUSEHOLD_FILE = "household_power_consumption.csv";
    public static final String ZONE_FILE = "powerconsumption.csv";

    /** Records that were loaded plus the quality report for the file they came from. */
    public static class Dataset<T> {
        public final List<T> records;
        public final DataQualityReport quality;

        Dataset(List<T> records, DataQualityReport quality) {
            this.records = records;
            this.quality = quality;
        }
    }

    // ---------------------------------------------------------------- file lookup

    /**
     * Finds a data file. Order: POWERGRIDX_DATA_DIR (env var), then the working directory and up to three parents,
     * so the server starts correctly from the project root, from /backend, or from an IDE.
     */
    public static Path resolveDataFile(String fileName) {
        List<Path> candidates = new ArrayList<>();
        String dir = System.getenv("POWERGRIDX_DATA_DIR");
        if (dir != null && !dir.isBlank()) {
            candidates.add(Paths.get(dir).resolve(fileName));
        }
        Path p = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 4 && p != null; i++, p = p.getParent()) {
            candidates.add(p.resolve(fileName));
        }
        for (Path c : candidates) {
            if (Files.isRegularFile(c)) return c;
        }
        throw new IllegalStateException("Data file '" + fileName + "' not found. Looked in: " + candidates
            + ". Set POWERGRIDX_DATA_DIR to the folder that contains the CSV files.");
    }

    // ---------------------------------------------------------------- household (1-minute meter data)

    public static Dataset<HouseholdRecord> loadHouseholdData(Path file) throws IOException {
        log.info("Loading household data from {}", file);
        DataQualityReport q = new DataQualityReport("household", file.getFileName().toString(), 1);
        List<HouseholdRecord> out = new ArrayList<>(270_000);
        GapTracker gaps = new GapTracker(q);

        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Integer> col = readHeader(in, file, "Date", "Time", "Global_active_power",
                "Global_reactive_power", "Voltage", "Global_intensity", "Sub_metering_1", "Sub_metering_2", "Sub_metering_3");
            int cDate = col.get("date"), cTime = col.get("time");
            int cGap = col.get("global_active_power"), cGrp = col.get("global_reactive_power");
            int cV = col.get("voltage"), cI = col.get("global_intensity");
            int cS1 = col.get("sub_metering_1"), cS2 = col.get("sub_metering_2"), cS3 = col.get("sub_metering_3");

            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                q.totalRows++;
                String[] f = splitCsv(line);

                LocalDateTime ts = f.length > Math.max(cDate, cTime) ? parseHouseholdTimestamp(f[cDate], f[cTime]) : null;
                if (ts == null) {
                    q.malformedRows++;
                    continue;
                }

                double[] v = new double[7];
                int[] idx = {cGap, cGrp, cV, cI, cS1, cS2, cS3};
                boolean complete = true;
                for (int i = 0; i < idx.length; i++) {
                    v[i] = idx[i] < f.length ? parseNumber(f[idx[i]]) : Double.NaN;
                    if (Double.isNaN(v[i])) { complete = false; break; }
                }
                if (!complete) {
                    q.missingValueRows++;
                    gaps.skipped(ts);
                    continue;
                }
                gaps.ok();

                HouseholdRecord r = new HouseholdRecord();
                r.timestamp = ts;
                r.globalActivePower = v[0];
                r.globalReactivePower = v[1];
                r.voltage = v[2];
                r.globalIntensity = v[3];
                r.subMetering1 = v[4];
                r.subMetering2 = v[5];
                r.subMetering3 = v[6];
                out.add(r);
                q.loadedRows++;
                if (q.firstTimestamp == null || ts.isBefore(q.firstTimestamp)) q.firstTimestamp = ts;
                if (q.lastTimestamp == null || ts.isAfter(q.lastTimestamp)) q.lastTimestamp = ts;
            }
        }
        gaps.ok();
        log.info("Household data: {} rows read, {} loaded, {} skipped ({} gaps)",
            q.totalRows, q.loadedRows, q.skippedRows(), q.gaps.size());
        return new Dataset<>(out, q);
    }

    // ---------------------------------------------------------------- zones (10-minute demand + weather)

    public static Dataset<ZoneRecord> loadZoneData(Path file) throws IOException {
        log.info("Loading zone data from {}", file);
        DataQualityReport q = new DataQualityReport("zones", file.getFileName().toString(), 10);
        List<ZoneRecord> out = new ArrayList<>(55_000);
        GapTracker gaps = new GapTracker(q);

        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Integer> col = readHeader(in, file, "Datetime", "Temperature", "Humidity", "WindSpeed",
                "GeneralDiffuseFlows", "DiffuseFlows", "PowerConsumption_Zone1", "PowerConsumption_Zone2", "PowerConsumption_Zone3");
            int cDt = col.get("datetime");
            int[] idx = {
                col.get("temperature"), col.get("humidity"), col.get("windspeed"),
                col.get("generaldiffuseflows"), col.get("diffuseflows"),
                col.get("powerconsumption_zone1"), col.get("powerconsumption_zone2"), col.get("powerconsumption_zone3")
            };

            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                q.totalRows++;
                String[] f = splitCsv(line);

                LocalDateTime ts = f.length > cDt ? parseZoneTimestamp(f[cDt]) : null;
                if (ts == null) {
                    q.malformedRows++;
                    continue;
                }

                double[] v = new double[idx.length];
                boolean complete = true;
                for (int i = 0; i < idx.length; i++) {
                    v[i] = idx[i] < f.length ? parseNumber(f[idx[i]]) : Double.NaN;
                    if (Double.isNaN(v[i])) { complete = false; break; }
                }
                if (!complete) {
                    q.missingValueRows++;
                    gaps.skipped(ts);
                    continue;
                }
                gaps.ok();

                ZoneRecord r = new ZoneRecord();
                r.timestamp = ts;
                r.temperature = v[0];
                r.humidity = v[1];
                r.windSpeed = v[2];
                r.generalDiffuseFlows = v[3];
                r.diffuseFlows = v[4];
                r.powerConsumptionZone1 = v[5];
                r.powerConsumptionZone2 = v[6];
                r.powerConsumptionZone3 = v[7];
                out.add(r);
                q.loadedRows++;
                if (q.firstTimestamp == null || ts.isBefore(q.firstTimestamp)) q.firstTimestamp = ts;
                if (q.lastTimestamp == null || ts.isAfter(q.lastTimestamp)) q.lastTimestamp = ts;
            }
        }
        gaps.ok();
        log.info("Zone data: {} rows read, {} loaded, {} skipped", q.totalRows, q.loadedRows, q.skippedRows());
        return new Dataset<>(out, q);
    }

    // ---------------------------------------------------------------- parsing helpers

    private static Map<String, Integer> readHeader(BufferedReader in, Path file, String... required) throws IOException {
        String header = in.readLine();
        if (header == null) throw new IOException("File is empty: " + file);
        if (!header.isEmpty() && header.charAt(0) == '﻿') header = header.substring(1); // strip BOM
        String[] names = splitCsv(header);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < names.length; i++) col.put(names[i].trim().toLowerCase(Locale.ROOT), i);
        for (String r : required) {
            if (!col.containsKey(r.toLowerCase(Locale.ROOT))) {
                throw new IOException("Column '" + r + "' not found in " + file.getFileName() + " (columns: " + String.join(", ", names) + ")");
            }
        }
        return col;
    }

    /** Minimal CSV splitter: commas, optional double quotes, "" as an escaped quote. */
    static String[] splitCsv(String line) {
        List<String> parts = new ArrayList<>(12);
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else quoted = false;
                } else cur.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                parts.add(cur.toString());
                cur.setLength(0);
            } else cur.append(c);
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    /** Returns NaN for blank values, "?" placeholders and anything that is not a finite number. */
    static double parseNumber(String s) {
        if (s == null) return Double.NaN;
        s = s.trim();
        if (s.isEmpty() || s.equals("?")) return Double.NaN;
        try {
            double d = Double.parseDouble(s);
            return Double.isFinite(d) ? d : Double.NaN;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** Household files mix "1/1/07" and "14/1/2007" (day/month/year) with "H:mm:ss" times. */
    static LocalDateTime parseHouseholdTimestamp(String date, String time) {
        try {
            String[] d = date.trim().split("/");
            String[] t = time.trim().split(":");
            if (d.length != 3 || t.length < 2) return null;
            int day = Integer.parseInt(d[0]), month = Integer.parseInt(d[1]), year = Integer.parseInt(d[2]);
            if (year < 100) year += 2000;
            int hour = Integer.parseInt(t[0]), min = Integer.parseInt(t[1]);
            int sec = t.length > 2 ? Integer.parseInt(t[2]) : 0;
            return LocalDateTime.of(year, month, day, hour, min, sec);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Zone files use "M/d/yyyy H:mm" (month/day/year). */
    static LocalDateTime parseZoneTimestamp(String s) {
        try {
            String[] parts = s.trim().split("\\s+");
            if (parts.length != 2) return null;
            String[] d = parts[0].split("/");
            String[] t = parts[1].split(":");
            if (d.length != 3 || t.length < 2) return null;
            int month = Integer.parseInt(d[0]), day = Integer.parseInt(d[1]), year = Integer.parseInt(d[2]);
            if (year < 100) year += 2000;
            return LocalDateTime.of(year, month, day, Integer.parseInt(t[0]), Integer.parseInt(t[1]));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Groups consecutive skipped rows into gaps so outages can be reported as time ranges. */
    private static class GapTracker {
        private final DataQualityReport report;
        private DataQualityReport.Gap open;

        GapTracker(DataQualityReport report) { this.report = report; }

        void skipped(LocalDateTime ts) {
            if (open == null) open = new DataQualityReport.Gap(ts);
            open.end = ts;
            open.rows++;
        }

        void ok() {
            if (open != null) {
                report.gaps.add(open);
                open = null;
            }
        }
    }
}
