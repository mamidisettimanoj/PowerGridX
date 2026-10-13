package com.powergridx.controllers;

import com.powergridx.algorithms.strings.AhoCorasick;
import com.powergridx.algorithms.strings.KMP;
import com.powergridx.algorithms.strings.RabinKarp;
import com.powergridx.algorithms.strings.ZFunction;
import com.powergridx.models.GridEvent;
import com.powergridx.services.DataStore;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Incident endpoints: browse grid events found in the meter data and search the incident log.
 * Searching runs on the server over the full log, so the browser never has to send the log text back.
 */
public class FaultController {

    private static final int MAX_LIMIT = 500;
    private static final int MAX_RESULT_LINES = 200;

    /** Lower-cased incident log (one event per line, oldest first) plus the offset where each line starts. */
    private static final class SearchIndex {
        final DataStore.Snapshot snapshot;
        final String text;
        final int[] lineStarts;

        SearchIndex(DataStore.Snapshot snapshot) {
            this.snapshot = snapshot;
            StringBuilder sb = new StringBuilder();
            lineStarts = new int[snapshot.events.size()];
            int i = 0;
            for (GridEvent e : snapshot.events) {
                lineStarts[i++] = sb.length();
                sb.append(e.toLogLine().toLowerCase(Locale.ROOT)).append('\n');
            }
            text = sb.toString();
        }

        int lineOf(int position) {
            int lo = 0, hi = lineStarts.length - 1, ans = 0;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (lineStarts[mid] <= position) { ans = mid; lo = mid + 1; }
                else hi = mid - 1;
            }
            return ans;
        }
    }

    private static volatile SearchIndex index;

    private static SearchIndex index() {
        DataStore.Snapshot snap = DataStore.get();
        SearchIndex idx = index;
        if (idx == null || idx.snapshot != snap) {
            idx = new SearchIndex(snap);
            index = idx;
        }
        return idx;
    }

    private static int intParam(Context ctx, String name, int def, int min, int max) {
        String raw = ctx.queryParam(name);
        if (raw == null || raw.isBlank()) return def;
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(raw.trim())));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** GET /api/faults/events?type=&severity=&limit=&offset=  (newest first) */
    public static void getEvents(Context ctx) {
        String type = ctx.queryParam("type");
        String severity = ctx.queryParam("severity");
        int limit = intParam(ctx, "limit", 50, 1, MAX_LIMIT);
        int offset = intParam(ctx, "offset", 0, 0, Integer.MAX_VALUE);

        List<GridEvent> all = DataStore.get().events;
        List<Map<String, Object>> page = new ArrayList<>();
        int matched = 0;
        for (int i = all.size() - 1; i >= 0; i--) {
            GridEvent e = all.get(i);
            if (type != null && !type.isBlank() && !type.equalsIgnoreCase("ALL") && !e.type.equalsIgnoreCase(type)) continue;
            if (severity != null && !severity.isBlank() && !severity.equalsIgnoreCase("ALL") && !e.severity.equalsIgnoreCase(severity)) continue;
            if (matched >= offset && page.size() < limit) page.add(e.toMap());
            matched++;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "success");
        out.put("total", matched);
        out.put("offset", offset);
        out.put("limit", limit);
        out.put("events", page);
        ctx.json(out);
    }

    /** GET /api/faults/search?q=...&algo=kmp|rabin-karp|z-function|aho-corasick  (case-insensitive) */
    public static void search(Context ctx) {
        String q = ctx.queryParam("q");
        String algo = ctx.queryParam("algo");
        if (algo == null || algo.isBlank()) algo = "kmp";
        algo = algo.toLowerCase(Locale.ROOT);

        if (q == null || q.isBlank()) {
            ctx.status(400).json(Map.of("status", "error", "message", "Enter a search term"));
            return;
        }
        if (q.length() > 200) {
            ctx.status(400).json(Map.of("status", "error", "message", "Search term is too long (max 200 characters)"));
            return;
        }

        SearchIndex idx = index();
        String needle = q.trim().toLowerCase(Locale.ROOT);
        List<Integer> starts = new ArrayList<>();
        String algorithmName;

        long t0 = System.nanoTime();
        switch (algo) {
            case "kmp" -> {
                algorithmName = "KMP";
                starts.addAll(KMP.search(idx.text, needle));
            }
            case "rabin-karp" -> {
                algorithmName = "Rabin-Karp";
                starts.addAll(RabinKarp.search(idx.text, needle));
            }
            case "z-function" -> {
                algorithmName = "Z-Function";
                starts.addAll(ZFunction.search(idx.text, needle));
            }
            case "aho-corasick" -> {
                algorithmName = "Aho-Corasick";
                List<String> patterns = new ArrayList<>();
                for (String p : needle.split(",")) {
                    if (!p.isBlank()) patterns.add(p.trim());
                }
                if (patterns.isEmpty()) {
                    ctx.status(400).json(Map.of("status", "error", "message", "Enter at least one term"));
                    return;
                }
                for (AhoCorasick.Match m : new AhoCorasick(patterns).search(idx.text)) starts.add(m.position);
            }
            default -> {
                ctx.status(400).json(Map.of("status", "error", "message", "Unknown algorithm: " + algo));
                return;
            }
        }
        long elapsed = System.nanoTime() - t0;

        TreeSet<Integer> lines = new TreeSet<>();
        for (int pos : starts) lines.add(idx.lineOf(pos));

        List<Map<String, Object>> results = new ArrayList<>();
        for (int line : lines) {
            if (results.size() >= MAX_RESULT_LINES) break;
            Map<String, Object> m = idx.snapshot.events.get(line).toMap();
            m.put("line", line + 1);
            results.add(m);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "success");
        out.put("algorithm", algorithmName);
        out.put("query", q.trim());
        out.put("logLines", idx.snapshot.events.size());
        out.put("occurrences", starts.size());
        out.put("matchedLines", lines.size());
        out.put("truncated", lines.size() > results.size());
        out.put("timeMicros", Math.round(elapsed / 10.0) / 100.0);
        out.put("results", results);
        ctx.json(out);
    }
}
