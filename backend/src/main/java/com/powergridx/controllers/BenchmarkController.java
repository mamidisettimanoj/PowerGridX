package com.powergridx.controllers;

import io.javalin.http.Context;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkController {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkController.class);

    public static void getResults(Context ctx) {
        try {
            // Read the JMH JSON output
            // Works whether the server is started from the project root or from /backend
            Path path = Paths.get("benchmark-results.json");
            if (!Files.exists(path)) path = Paths.get("backend", "benchmark-results.json");
            if (Files.exists(path)) {
                String content = Files.readString(path);
                ctx.contentType("application/json").result(content);
            } else {
                ctx.status(404).json(Map.of(
                    "status", "error",
                    "message", "Benchmark results not yet generated. Please run benchmarks."
                ));
            }
        } catch (Exception e) {
            log.error("Failed to read benchmark results", e);
            ctx.status(500).json(Map.of("status", "error", "message", "Internal server error"));
        }
    }
}
