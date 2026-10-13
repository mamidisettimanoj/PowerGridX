package com.powergridx.algorithms.flow;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Dinic {
    public static int maxFlow(int[][] capacity, int source, int sink) {
        int n = capacity.length;
        int[][] residual = new int[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(capacity[i], 0, residual[i], 0, n);
        }

        int maxFlow = 0;
        int[] level = new int[n];

        while (bfs(residual, source, sink, level)) {
            int[] ptr = new int[n];
            while (true) {
                int pushed = dfs(residual, level, ptr, source, sink, Integer.MAX_VALUE);
                if (pushed == 0) break;
                maxFlow += pushed;
            }
        }

        return maxFlow;
    }

    private static boolean bfs(int[][] residual, int source, int sink, int[] level) {
        Arrays.fill(level, -1);
        level[source] = 0;
        Queue<Integer> queue = new LinkedList<>();
        queue.add(source);

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v = 0; v < residual.length; v++) {
                if (level[v] < 0 && residual[u][v] > 0) {
                    level[v] = level[u] + 1;
                    queue.add(v);
                }
            }
        }
        return level[sink] >= 0;
    }

    private static int dfs(int[][] residual, int[] level, int[] ptr, int u, int sink, int pushed) {
        if (pushed == 0) return 0;
        if (u == sink) return pushed;

        for (int cid = ptr[u]; cid < residual.length; cid++, ptr[u]++) {
            int v = cid;
            if (level[u] + 1 != level[v] || residual[u][v] == 0) continue;

            int tr = dfs(residual, level, ptr, v, sink, Math.min(pushed, residual[u][v]));
            if (tr == 0) continue;

            residual[u][v] -= tr;
            residual[v][u] += tr;
            return tr;
        }
        return 0;
    }
}
