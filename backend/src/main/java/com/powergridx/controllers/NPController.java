package com.powergridx.controllers;

import io.javalin.http.Context;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import com.powergridx.algorithms.np.VertexCover;

public class NPController {

    // Simple conflict graph between 8 infrastructure nodes
    private static final List<VertexCover.Edge> conflictGraph = Arrays.asList(
        new VertexCover.Edge(0, 1),
        new VertexCover.Edge(0, 2),
        new VertexCover.Edge(1, 3),
        new VertexCover.Edge(1, 4),
        new VertexCover.Edge(2, 4),
        new VertexCover.Edge(3, 5),
        new VertexCover.Edge(4, 5),
        new VertexCover.Edge(4, 6),
        new VertexCover.Edge(5, 7),
        new VertexCover.Edge(6, 7)
    );

    public static void vertexCover(Context ctx) {
        long startApprox = System.nanoTime();
        List<Integer> approxCover = VertexCover.approxVertexCover(8, conflictGraph);
        long endApprox = System.nanoTime();
        
        long startExact = System.nanoTime();
        List<Integer> exactCover = VertexCover.exactVertexCover(8, conflictGraph);
        long endExact = System.nanoTime();

        ctx.json(Map.of(
            "status", "success",
            "nodes", 8,
            "approxCover", approxCover,
            "approxTimeNs", (endApprox - startApprox),
            "exactCover", exactCover,
            "exactTimeNs", (endExact - startExact)
        ));
    }
}
