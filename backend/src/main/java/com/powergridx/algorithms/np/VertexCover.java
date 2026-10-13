package com.powergridx.algorithms.np;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VertexCover {

    public static class Edge {
        public int u, v;
        public Edge(int u, int v) {
            this.u = u;
            this.v = v;
        }
    }

    // 2-Approximation for Vertex Cover using maximal matching
    public static List<Integer> approxVertexCover(int n, List<Edge> edges) {
        List<Integer> cover = new ArrayList<>();
        boolean[] visited = new boolean[n];
        
        for (Edge e : edges) {
            if (!visited[e.u] && !visited[e.v]) {
                visited[e.u] = true;
                visited[e.v] = true;
                cover.add(e.u);
                cover.add(e.v);
            }
        }
        
        return cover;
    }

    // Exact Vertex Cover using Backtracking (exponential time, suitable for small graphs)
    public static List<Integer> exactVertexCover(int n, List<Edge> edges) {
        List<Integer> minCover = new ArrayList<>();
        for (int i = 0; i < n; i++) minCover.add(i); // max cover is all nodes

        // Test all 2^n combinations
        for (int i = 0; i < (1 << n); i++) {
            List<Integer> currentCover = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) != 0) {
                    currentCover.add(j);
                }
            }

            if (isValidCover(currentCover, edges) && currentCover.size() < minCover.size()) {
                minCover = new ArrayList<>(currentCover);
            }
        }

        return minCover;
    }

    private static boolean isValidCover(List<Integer> cover, List<Edge> edges) {
        Set<Integer> coverSet = new HashSet<>(cover);
        for (Edge e : edges) {
            if (!coverSet.contains(e.u) && !coverSet.contains(e.v)) {
                return false;
            }
        }
        return true;
    }
}
