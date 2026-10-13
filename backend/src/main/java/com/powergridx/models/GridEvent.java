package com.powergridx.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** An operating-limit violation detected in the household meter data. */
public class GridEvent {

    /** Operating limits used for detection. Exposed through the API so the UI can show them. */
    public static final double UNDER_VOLTAGE_LIMIT_V = 225.0;
    public static final double OVER_VOLTAGE_LIMIT_V = 250.0;
    public static final double OVERCURRENT_LIMIT_A = 40.0;
    public static final double LOAD_SURGE_LIMIT_KW = 8.0;

    public static final String UNDER_VOLTAGE = "UNDER_VOLTAGE";
    public static final String OVER_VOLTAGE = "OVER_VOLTAGE";
    public static final String OVERCURRENT = "OVERCURRENT";
    public static final String LOAD_SURGE = "LOAD_SURGE";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public final LocalDateTime timestamp;
    public final String type;
    public final String severity;   // CRITICAL, WARNING, NOTICE
    public final double value;
    public final String unit;
    public final double limit;
    public final String message;

    public GridEvent(LocalDateTime timestamp, String type, String severity, double value, String unit, double limit, String message) {
        this.timestamp = timestamp;
        this.type = type;
        this.severity = severity;
        this.value = value;
        this.unit = unit;
        this.limit = limit;
        this.message = message;
    }

    /** Single-line text form used for the searchable incident log. */
    public String toLogLine() {
        return FMT.format(timestamp) + " | " + type + " | " + severity + " | " + message;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", timestamp.toString());
        m.put("type", type);
        m.put("severity", severity);
        m.put("value", value);
        m.put("unit", unit);
        m.put("limit", limit);
        m.put("message", message);
        return m;
    }

    /** Scans readings in order and returns every limit violation (one reading can raise more than one). */
    public static List<GridEvent> detect(List<HouseholdRecord> records) {
        List<GridEvent> events = new ArrayList<>();
        for (HouseholdRecord r : records) {
            if (r.voltage < UNDER_VOLTAGE_LIMIT_V) {
                events.add(new GridEvent(r.timestamp, UNDER_VOLTAGE, "WARNING", r.voltage, "V", UNDER_VOLTAGE_LIMIT_V,
                    String.format(Locale.ROOT, "Voltage fell to %.2f V (minimum %.0f V)", r.voltage, UNDER_VOLTAGE_LIMIT_V)));
            }
            if (r.voltage > OVER_VOLTAGE_LIMIT_V) {
                events.add(new GridEvent(r.timestamp, OVER_VOLTAGE, "CRITICAL", r.voltage, "V", OVER_VOLTAGE_LIMIT_V,
                    String.format(Locale.ROOT, "Voltage reached %.2f V (maximum %.0f V)", r.voltage, OVER_VOLTAGE_LIMIT_V)));
            }
            if (r.globalIntensity > OVERCURRENT_LIMIT_A) {
                events.add(new GridEvent(r.timestamp, OVERCURRENT, "WARNING", r.globalIntensity, "A", OVERCURRENT_LIMIT_A,
                    String.format(Locale.ROOT, "Current reached %.1f A (maximum %.0f A)", r.globalIntensity, OVERCURRENT_LIMIT_A)));
            }
            if (r.globalActivePower > LOAD_SURGE_LIMIT_KW) {
                events.add(new GridEvent(r.timestamp, LOAD_SURGE, "NOTICE", r.globalActivePower, "kW", LOAD_SURGE_LIMIT_KW,
                    String.format(Locale.ROOT, "Active power reached %.2f kW (surge threshold %.0f kW)", r.globalActivePower, LOAD_SURGE_LIMIT_KW)));
            }
        }
        return events;
    }
}
