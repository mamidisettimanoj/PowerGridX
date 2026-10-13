package com.powergridx.controllers;

import io.javalin.http.Context;
import java.util.Map;
import com.powergridx.algorithms.dp.Levenshtein;
import com.powergridx.algorithms.dp.BitmaskDP;

public class OptimizationController {
    
    public static void fuzzySearch(Context ctx) {
        String query = ctx.queryParam("query");
        String target = ctx.queryParam("target");
        if (query == null || target == null) {
            ctx.status(400).json(Map.of("error", "Missing query or target"));
            return;
        }
        
        long start = System.nanoTime();
        int dist = Levenshtein.distance(query, target);
        long end = System.nanoTime();
        
        ctx.json(Map.of(
            "algorithm", "Levenshtein DP",
            "distance", dist,
            "timeNs", (end - start)
        ));
    }

    public static void droneRouting(Context ctx) {
        // Hardcoded example graph for drone inspection (4 substations)
        int[][] dist = {
            {0, 10, 15, 20},
            {10, 0, 35, 25},
            {15, 35, 0, 30},
            {20, 25, 30, 0}
        };
        
        long start = System.nanoTime();
        int minCost = BitmaskDP.tsp(dist, 4);
        long end = System.nanoTime();
        
        ctx.json(Map.of(
            "algorithm", "Bitmask DP (TSP)",
            "minCost", minCost,
            "timeNs", (end - start)
        ));
    }
}
