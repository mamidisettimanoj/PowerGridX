package com.powergridx.services;

import com.powergridx.models.GridEvent;
import com.powergridx.models.HouseholdRecord;
import com.powergridx.models.ZoneRecord;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Computes the aggregates shown on the dashboard from the loaded snapshot. All numbers come from the CSV data. */
public final class DashboardService {

    private static final double ZONE_INTERVAL_HOURS = 10.0 / 60.0;   // zone rows are 10-minute averages in kW
    private static final double HOUSEHOLD_INTERVAL_HOURS = 1.0 / 60.0; // household rows are 1-minute averages in kW

    private DashboardService() {}

    static double round(double v, int places) {
        double f = Math.pow(10, places);
        return Math.round(v * f) / f;
    }

    // ------------------------------------------------------------------ overview

    public static Map<String, Object> overview() {
        DataStore.Snapshot s = DataStore.get();

        // zone demand
        double z1 = 0, z2 = 0, z3 = 0, temp = 0, hum = 0, wind = 0;
        double peakZoneKw = -1;
        LocalDateTime peakZoneAt = null;
        double minTemp = Double.MAX_VALUE, maxTemp = -Double.MAX_VALUE;
        for (ZoneRecord z : s.zones) {
            z1 += z.powerConsumptionZone1;
            z2 += z.powerConsumptionZone2;
            z3 += z.powerConsumptionZone3;
            temp += z.temperature;
            hum += z.humidity;
            wind += z.windSpeed;
            minTemp = Math.min(minTemp, z.temperature);
            maxTemp = Math.max(maxTemp, z.temperature);
            double total = z.powerConsumptionZone1 + z.powerConsumptionZone2 + z.powerConsumptionZone3;
            if (total > peakZoneKw) {
                peakZoneKw = total;
                peakZoneAt = z.timestamp;
            }
        }
        int zn = s.zones.size();
        double zone1Mwh = z1 * ZONE_INTERVAL_HOURS / 1000.0;
        double zone2Mwh = z2 * ZONE_INTERVAL_HOURS / 1000.0;
        double zone3Mwh = z3 * ZONE_INTERVAL_HOURS / 1000.0;

        Map<String, Object> zones = new LinkedHashMap<>();
        Map<String, Object> energy = new LinkedHashMap<>();
        energy.put("zone1", round(zone1Mwh, 1));
        energy.put("zone2", round(zone2Mwh, 1));
        energy.put("zone3", round(zone3Mwh, 1));
        energy.put("total", round(zone1Mwh + zone2Mwh + zone3Mwh, 1));
        zones.put("energyMWh", energy);
        zones.put("avgDemandKw", round((z1 + z2 + z3) / zn, 1));
        zones.put("peakDemandKw", round(peakZoneKw, 1));
        zones.put("peakDemandAt", peakZoneAt == null ? null : peakZoneAt.toString());
        zones.put("avgTemperatureC", round(temp / zn, 1));
        zones.put("minTemperatureC", round(minTemp, 1));
        zones.put("maxTemperatureC", round(maxTemp, 1));
        zones.put("avgHumidityPct", round(hum / zn, 1));
        zones.put("avgWindSpeed", round(wind / zn, 2));

        // household meter
        double power = 0, volt = 0, peakKw = -1;
        double minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        LocalDateTime peakAt = null;
        for (HouseholdRecord r : s.household) {
            power += r.globalActivePower;
            volt += r.voltage;
            minV = Math.min(minV, r.voltage);
            maxV = Math.max(maxV, r.voltage);
            if (r.globalActivePower > peakKw) {
                peakKw = r.globalActivePower;
                peakAt = r.timestamp;
            }
        }
        int hn = s.household.size();
        Map<String, Object> household = new LinkedHashMap<>();
        household.put("avgLoadKw", round(power / hn, 3));
        household.put("peakLoadKw", round(peakKw, 2));
        household.put("peakLoadAt", peakAt == null ? null : peakAt.toString());
        household.put("energyKwh", round(power * HOUSEHOLD_INTERVAL_HOURS, 0));
        household.put("avgVoltage", round(volt / hn, 2));
        household.put("minVoltage", round(minV, 2));
        household.put("maxVoltage", round(maxV, 2));

        // events
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (String t : new String[]{GridEvent.OVER_VOLTAGE, GridEvent.UNDER_VOLTAGE, GridEvent.OVERCURRENT, GridEvent.LOAD_SURGE}) {
            byType.put(t, 0);
        }
        Map<String, Integer> bySeverity = new LinkedHashMap<>();
        for (String t : new String[]{"CRITICAL", "WARNING", "NOTICE"}) bySeverity.put(t, 0);
        for (GridEvent e : s.events) {
            byType.merge(e.type, 1, Integer::sum);
            bySeverity.merge(e.severity, 1, Integer::sum);
        }
        Map<String, Object> thresholds = new LinkedHashMap<>();
        thresholds.put("underVoltageV", GridEvent.UNDER_VOLTAGE_LIMIT_V);
        thresholds.put("overVoltageV", GridEvent.OVER_VOLTAGE_LIMIT_V);
        thresholds.put("overcurrentA", GridEvent.OVERCURRENT_LIMIT_A);
        thresholds.put("loadSurgeKw", GridEvent.LOAD_SURGE_LIMIT_KW);
        Map<String, Object> events = new LinkedHashMap<>();
        events.put("total", s.events.size());
        events.put("byType", byType);
        events.put("bySeverity", bySeverity);
        events.put("thresholds", thresholds);

        Map<String, Object> quality = new LinkedHashMap<>();
        quality.put("household", s.householdQuality.toMap(5));
        quality.put("zones", s.zoneQuality.toMap(5));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "success");
        out.put("loadedAt", s.loadedAt.toString());
        out.put("zones", zones);
        out.put("household", household);
        out.put("events", events);
        out.put("dataQuality", quality);
        return out;
    }

    // ------------------------------------------------------------------ zone energy over time

    /** Zone energy per day, week (starting Monday) or month, in MWh, with average weather for the same period. */
    public static Map<String, Object> zoneSeries(String granularity) {
        if (!granularity.equals("day") && !granularity.equals("week") && !granularity.equals("month")) {
            throw new IllegalArgumentException("granularity must be one of: day, week, month");
        }
        DataStore.Snapshot s = DataStore.get();
        // [zone1 kW sum, zone2, zone3, temperature sum, humidity sum, count]
        TreeMap<LocalDate, double[]> buckets = new TreeMap<>();
        for (ZoneRecord z : s.zones) {
            LocalDate d = z.timestamp.toLocalDate();
            LocalDate key = switch (granularity) {
                case "week" -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case "month" -> d.withDayOfMonth(1);
                default -> d;
            };
            double[] b = buckets.computeIfAbsent(key, k -> new double[6]);
            b[0] += z.powerConsumptionZone1;
            b[1] += z.powerConsumptionZone2;
            b[2] += z.powerConsumptionZone3;
            b[3] += z.temperature;
            b[4] += z.humidity;
            b[5]++;
        }
        List<Map<String, Object>> points = new ArrayList<>(buckets.size());
        for (Map.Entry<LocalDate, double[]> e : buckets.entrySet()) {
            double[] b = e.getValue();
            double m1 = b[0] * ZONE_INTERVAL_HOURS / 1000.0;
            double m2 = b[1] * ZONE_INTERVAL_HOURS / 1000.0;
            double m3 = b[2] * ZONE_INTERVAL_HOURS / 1000.0;
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", e.getKey().toString());
            p.put("zone1", round(m1, 2));
            p.put("zone2", round(m2, 2));
            p.put("zone3", round(m3, 2));
            p.put("total", round(m1 + m2 + m3, 2));
            p.put("avgTemperatureC", round(b[3] / b[5], 1));
            p.put("avgHumidityPct", round(b[4] / b[5], 1));
            p.put("samples", (int) b[5]);
            points.add(p);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "success");
        out.put("granularity", granularity);
        out.put("unit", "MWh");
        out.put("points", points);
        return out;
    }

    // ------------------------------------------------------------------ household load by hour of day

    public static Map<String, Object> loadProfile() {
        DataStore.Snapshot s = DataStore.get();
        double[][] kw = new double[2][24];    // [0] weekday, [1] weekend
        int[][] n = new int[2][24];
        double[] volt = new double[24];
        int[] vn = new int[24];
        for (HouseholdRecord r : s.household) {
            int h = r.timestamp.getHour();
            DayOfWeek dow = r.timestamp.getDayOfWeek();
            int k = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) ? 1 : 0;
            kw[k][h] += r.globalActivePower;
            n[k][h]++;
            volt[h] += r.voltage;
            vn[h]++;
        }
        List<Map<String, Object>> points = new ArrayList<>(24);
        int peakHour = 0;
        double peakAvg = -1;
        for (int h = 0; h < 24; h++) {
            double weekday = n[0][h] == 0 ? 0 : kw[0][h] / n[0][h];
            double weekend = n[1][h] == 0 ? 0 : kw[1][h] / n[1][h];
            double all = (n[0][h] + n[1][h]) == 0 ? 0 : (kw[0][h] + kw[1][h]) / (n[0][h] + n[1][h]);
            if (all > peakAvg) {
                peakAvg = all;
                peakHour = h;
            }
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("hour", h);
            p.put("label", String.format("%02d:00", h));
            p.put("avgLoadKw", round(all, 3));
            p.put("weekdayKw", round(weekday, 3));
            p.put("weekendKw", round(weekend, 3));
            p.put("avgVoltage", round(vn[h] == 0 ? 0 : volt[h] / vn[h], 2));
            points.add(p);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "success");
        out.put("unit", "kW");
        out.put("peakHour", peakHour);
        out.put("peakAvgLoadKw", round(peakAvg, 3));
        out.put("points", points);
        return out;
    }

    // ------------------------------------------------------------------ voltage per day

    public static Map<String, Object> voltageTrend() {
        DataStore.Snapshot s = DataStore.get();
        // [min, max, sum, count, kW sum]
        TreeMap<LocalDate, double[]> days = new TreeMap<>();
        for (HouseholdRecord r : s.household) {
            double[] b = days.computeIfAbsent(r.timestamp.toLocalDate(),
                k -> new double[]{Double.MAX_VALUE, -Double.MAX_VALUE, 0, 0, 0});
            b[0] = Math.min(b[0], r.voltage);
            b[1] = Math.max(b[1], r.voltage);
            b[2] += r.voltage;
            b[3]++;
            b[4] += r.globalActivePower;
        }
        List<Map<String, Object>> points = new ArrayList<>(days.size());
        for (Map.Entry<LocalDate, double[]> e : days.entrySet()) {
            double[] b = e.getValue();
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", e.getKey().toString());
            p.put("minVoltage", round(b[0], 2));
            p.put("avgVoltage", round(b[2] / b[3], 2));
            p.put("maxVoltage", round(b[1], 2));
            p.put("energyKwh", round(b[4] * HOUSEHOLD_INTERVAL_HOURS, 2));
            p.put("samples", (int) b[3]);
            points.add(p);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "success");
        out.put("underVoltageLimitV", GridEvent.UNDER_VOLTAGE_LIMIT_V);
        out.put("overVoltageLimitV", GridEvent.OVER_VOLTAGE_LIMIT_V);
        out.put("points", points);
        return out;
    }
}
