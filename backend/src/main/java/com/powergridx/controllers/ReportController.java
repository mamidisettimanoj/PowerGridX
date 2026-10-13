package com.powergridx.controllers;

import com.powergridx.algorithms.suffix.LCP;
import com.powergridx.algorithms.suffix.SuffixArray;
import com.powergridx.services.DashboardService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ReportController {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    private static String date(Object iso) {
        return iso == null ? "n/a" : iso.toString().substring(0, 10);
    }

    /**
     * Builds a maintenance report from the loaded datasets and indexes it with a suffix array + LCP array,
     * which is the structure behind fast substring search over long reports.
     */
    public static void buildSuffixStructures(Context ctx) {
        Map<String, Object> o = DashboardService.overview();
        Map<String, Object> zones = map(o.get("zones"));
        Map<String, Object> energy = map(zones.get("energyMWh"));
        Map<String, Object> hh = map(o.get("household"));
        Map<String, Object> ev = map(o.get("events"));
        Map<String, Object> byType = map(ev.get("byType"));
        Map<String, Object> bySeverity = map(ev.get("bySeverity"));
        Map<String, Object> th = map(ev.get("thresholds"));
        Map<String, Object> dq = map(o.get("dataQuality"));
        Map<String, Object> hq = map(dq.get("household"));
        Map<String, Object> zq = map(dq.get("zones"));

        StringBuilder sb = new StringBuilder();
        sb.append("POWERGRIDX MAINTENANCE REPORT\n");
        sb.append(String.format(Locale.ROOT, "Household meter period: %s to %s (%s readings used).\n",
            date(hq.get("from")), date(hq.get("to")), hq.get("loadedRows")));
        sb.append(String.format(Locale.ROOT, "Zone demand period: %s to %s (%s readings used).\n\n",
            date(zq.get("from")), date(zq.get("to")), zq.get("loadedRows")));

        sb.append("VOLTAGE QUALITY:\n");
        sb.append(String.format(Locale.ROOT, "Average voltage: %.2f V. Range: %.2f V to %.2f V.\n",
            hh.get("avgVoltage"), hh.get("minVoltage"), hh.get("maxVoltage")));
        sb.append(String.format(Locale.ROOT, "Under-voltage events (below %.0f V): %s occurrences.\n",
            th.get("underVoltageV"), byType.get("UNDER_VOLTAGE")));
        sb.append(String.format(Locale.ROOT, "Over-voltage events (above %.0f V): %s occurrences.\n\n",
            th.get("overVoltageV"), byType.get("OVER_VOLTAGE")));

        sb.append("HOUSEHOLD LOAD:\n");
        sb.append(String.format(Locale.ROOT, "Average active power: %.3f kW.\n", hh.get("avgLoadKw")));
        sb.append(String.format(Locale.ROOT, "Peak load: %.2f kW on %s.\n", hh.get("peakLoadKw"), date(hh.get("peakLoadAt"))));
        sb.append(String.format(Locale.ROOT, "Overcurrent events (above %.0f A): %s occurrences.\n",
            th.get("overcurrentA"), byType.get("OVERCURRENT")));
        sb.append(String.format(Locale.ROOT, "Load surges (above %.0f kW): %s occurrences.\n\n",
            th.get("loadSurgeKw"), byType.get("LOAD_SURGE")));

        sb.append("ZONE DEMAND:\n");
        sb.append(String.format(Locale.ROOT, "Zone 1 energy: %,.1f MWh.\n", energy.get("zone1")));
        sb.append(String.format(Locale.ROOT, "Zone 2 energy: %,.1f MWh.\n", energy.get("zone2")));
        sb.append(String.format(Locale.ROOT, "Zone 3 energy: %,.1f MWh.\n", energy.get("zone3")));
        sb.append(String.format(Locale.ROOT, "Combined energy: %,.1f MWh.\n", energy.get("total")));
        sb.append(String.format(Locale.ROOT, "Peak combined demand: %,.1f kW on %s.\n\n",
            zones.get("peakDemandKw"), date(zones.get("peakDemandAt"))));

        sb.append("DATA QUALITY:\n");
        sb.append(String.format(Locale.ROOT, "Household meter completeness: %.2f%% (%s readings missing).\n",
            hq.get("completenessPct"), hq.get("skippedRows")));
        sb.append(String.format(Locale.ROOT, "Zone demand completeness: %.2f%%.\n\n", zq.get("completenessPct")));

        int critical = ((Number) bySeverity.get("CRITICAL")).intValue();
        int warning = ((Number) bySeverity.get("WARNING")).intValue();
        sb.append("ASSESSMENT: ");
        if (critical > 0) {
            sb.append(String.format(Locale.ROOT, "ACTION REQUIRED - %d over-voltage events recorded. Inspect voltage regulation equipment.", critical));
        } else if (warning > 0) {
            sb.append(String.format(Locale.ROOT, "WATCH - %d warning events recorded. Review at the next scheduled inspection.", warning));
        } else {
            sb.append("NORMAL - No operating limit violations recorded.");
        }

        String report = sb.toString();

        long start = System.nanoTime();
        int[] sa = SuffixArray.buildSuffixArray(report);
        int[] lcp = LCP.kasai(report, sa);
        long end = System.nanoTime();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "success");
        out.put("title", "Maintenance Report");
        out.put("period", date(hq.get("from")) + " to " + date(hq.get("to")));
        out.put("generatedOn", LocalDate.now().toString());
        out.put("text", report);
        out.put("sa", sa);
        out.put("lcp", lcp);
        out.put("timeNs", end - start);
        ctx.json(out);
    }
}
