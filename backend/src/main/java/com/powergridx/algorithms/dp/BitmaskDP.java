package com.powergridx.algorithms.dp;

import java.util.Arrays;

public class BitmaskDP {
    // Traveling Salesperson Problem style bitmask DP for routing inspection drones
    public static int tsp(int[][] dist, int n) {
        if (n <= 1) return 0;
        int[][] dp = new int[1 << n][n];
        for (int[] row : dp) {
            Arrays.fill(row, Integer.MAX_VALUE / 2); // Avoid overflow
        }
        
        dp[1][0] = 0; // Start at node 0
        
        for (int mask = 1; mask < (1 << n); mask++) {
            for (int u = 0; u < n; u++) {
                // If u is in the mask
                if ((mask & (1 << u)) != 0) {
                    for (int v = 0; v < n; v++) {
                        // If v is NOT in the mask, we can travel from u to v
                        if ((mask & (1 << v)) == 0) {
                            int nextMask = mask | (1 << v);
                            dp[nextMask][v] = Math.min(dp[nextMask][v], dp[mask][u] + dist[u][v]);
                        }
                    }
                }
            }
        }
        
        // Find minimum cost to return to start
        int minCost = Integer.MAX_VALUE;
        for (int i = 1; i < n; i++) {
            minCost = Math.min(minCost, dp[(1 << n) - 1][i] + dist[i][0]);
        }
        
        return minCost;
    }
}
