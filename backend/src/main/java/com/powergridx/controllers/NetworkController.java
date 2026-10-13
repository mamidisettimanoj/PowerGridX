package com.powergridx.controllers;

import io.javalin.http.Context;
import java.util.Map;
import com.powergridx.algorithms.flow.EdmondsKarp;
import com.powergridx.algorithms.flow.Dinic;

public class NetworkController {

    // Simple 6-node grid graph (0: Generator, 5: City Sink)
    private static final int[][] gridCapacities = {
        {0, 16, 13, 0, 0, 0},
        {0, 0, 10, 12, 0, 0},
        {0, 4, 0, 0, 14, 0},
        {0, 0, 9, 0, 0, 20},
        {0, 0, 0, 7, 0, 4},
        {0, 0, 0, 0, 0, 0}
    };

    public static void computeFlow(Context ctx) {
        String algo = ctx.queryParam("algo");
        if (algo == null || algo.isBlank()) algo = "edmonds-karp";
        algo = algo.toLowerCase();
        if (!algo.equals("edmonds-karp") && !algo.equals("dinic")) {
            ctx.status(400).json(Map.of("status", "error", "message", "Unknown algorithm: " + algo + " (use edmonds-karp or dinic)"));
            return;
        }
        
        long start = System.nanoTime();
        int maxFlow;
        if (algo.equals("dinic")) {
            maxFlow = Dinic.maxFlow(gridCapacities, 0, 5);
        } else {
            maxFlow = EdmondsKarp.maxFlow(gridCapacities, 0, 5);
        }
        long end = System.nanoTime();
        
        ctx.json(Map.of(
            "status", "success",
            "algorithm", algo,
            "maxFlow", maxFlow,
            "timeNs", (end - start),
            "capacities", gridCapacities
        ));
    }
}
