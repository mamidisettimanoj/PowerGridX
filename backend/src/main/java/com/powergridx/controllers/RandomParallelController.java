package com.powergridx.controllers;

import com.powergridx.algorithms.parallel.Parallel;
import com.powergridx.algorithms.parallel.Randomized;
import com.powergridx.models.HouseholdRecord;
import com.powergridx.services.DataStore;
import io.javalin.http.Context;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Telemetry processing over every household meter reading that was loaded. */
public class RandomParallelController {

    private static final int WARM_UP_RUNS = 3;
    private static final int TIMED_RUNS = 5;

    public static void process(Context ctx) {
        List<HouseholdRecord> records = DataStore.get().household;
        int n = records.size();

        double[] powerData = new double[n];
        for (int i = 0; i < n; i++) {
            powerData[i] = records.get(i).globalActivePower;
        }

        // 1. Reservoir sampling: uniform sample of k readings in one pass
        int sampleSize = Math.min(10, n);
        long start1 = System.nanoTime();
        double[] sample = Randomized.reservoirSampling(powerData, sampleSize);
        long end1 = System.nanoTime();

        // 2. Sequential vs parallel sum. Warm up first so JIT compilation does not skew the comparison,
        //    then report the average of several timed runs.
        double sumSeq = 0, sumPar = 0;
        for (int i = 0; i < WARM_UP_RUNS; i++) {
            sumSeq = Parallel.sequentialReduce(powerData);
            sumPar = Parallel.parallelReduce(powerData);
        }
        long seqTotal = 0, parTotal = 0;
        for (int i = 0; i < TIMED_RUNS; i++) {
            long a = System.nanoTime();
            sumSeq = Parallel.sequentialReduce(powerData);
            long b = System.nanoTime();
            sumPar = Parallel.parallelReduce(powerData);
            long c = System.nanoTime();
            seqTotal += b - a;
            parTotal += c - b;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("dataSize", n);
        response.put("cores", Runtime.getRuntime().availableProcessors());
        response.put("timedRuns", TIMED_RUNS);
        response.put("sample", sample);
        response.put("sampleTimeNs", end1 - start1);
        response.put("seqSum", sumSeq);
        response.put("seqTimeNs", seqTotal / TIMED_RUNS);
        response.put("parSum", sumPar);
        response.put("parTimeNs", parTotal / TIMED_RUNS);
        ctx.json(response);
    }
}
