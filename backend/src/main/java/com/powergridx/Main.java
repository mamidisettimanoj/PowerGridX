package com.powergridx;

import com.powergridx.services.DataStore;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting PowerGridX backend engine...");

        // Load the CSV data BEFORE accepting requests. If a file is missing or unusable the server stops here
        // with a clear message instead of serving an empty dashboard.
        try {
            DataStore.load();
        } catch (Exception e) {
            log.error("Cannot start: datasets failed to load. {}", e.getMessage());
            System.exit(1);
            return;
        }

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        }).start(port);

        app.get("/", ctx -> ctx.result("PowerGridX API Engine is running"));
        app.get("/api/health", ctx -> {
            var s = DataStore.get();
            Map<String, Object> health = new LinkedHashMap<>();
            health.put("status", "ok");
            health.put("householdRows", s.household.size());
            health.put("zoneRows", s.zones.size());
            health.put("gridEvents", s.events.size());
            health.put("householdTo", s.householdQuality.lastTimestamp.toString());
            health.put("zonesTo", s.zoneQuality.lastTimestamp.toString());
            health.put("loadedAt", s.loadedAt.toString());
            ctx.json(health);
        });

        // Dashboard
        app.get("/api/dashboard/overview", com.powergridx.controllers.AnalyticsController::getOverview);
        app.get("/api/dashboard/zones/series", com.powergridx.controllers.AnalyticsController::getZoneSeries);
        app.get("/api/dashboard/household/load-profile", com.powergridx.controllers.AnalyticsController::getLoadProfile);
        app.get("/api/dashboard/household/voltage-trend", com.powergridx.controllers.AnalyticsController::getVoltageTrend);

        // Incident intelligence (events found in the meter data + string-search over the incident log)
        app.get("/api/faults/events", com.powergridx.controllers.FaultController::getEvents);
        app.get("/api/faults/search", com.powergridx.controllers.FaultController::search);

        // String search primitives
        app.get("/api/dsa/kmp", com.powergridx.controllers.DSAController::searchKMP);
        app.get("/api/dsa/rabin-karp", com.powergridx.controllers.DSAController::searchRabinKarp);
        app.get("/api/dsa/z-function", com.powergridx.controllers.DSAController::searchZFunction);
        app.get("/api/dsa/aho-corasick", com.powergridx.controllers.DSAController::searchAhoCorasick);

        // Maintenance reports
        app.get("/api/reports/suffix", com.powergridx.controllers.ReportController::buildSuffixStructures);

        // Maintenance optimization
        app.get("/api/optimization/fuzzy", com.powergridx.controllers.OptimizationController::fuzzySearch);
        app.get("/api/optimization/drone", com.powergridx.controllers.OptimizationController::droneRouting);

        // Grid capacity
        app.get("/api/network/flow", com.powergridx.controllers.NetworkController::computeFlow);

        // Crew scheduling
        app.get("/api/np/vertex-cover", com.powergridx.controllers.NPController::vertexCover);

        // Telemetry processing
        app.get("/api/parallel/process", com.powergridx.controllers.RandomParallelController::process);

        // Benchmarks
        app.get("/api/benchmarks", com.powergridx.controllers.BenchmarkController::getResults);

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Unhandled exception", e);
            ctx.status(500).json(Map.of("status", "error", "message", "Internal Server Error"));
        });

        log.info("PowerGridX API listening on http://localhost:{}", port);
    }
}
