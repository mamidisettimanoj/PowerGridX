package com.powergridx.benchmarks;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;
import com.powergridx.algorithms.strings.KMP;
import com.powergridx.algorithms.strings.RabinKarp;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class StringSearchBenchmark {

    private String text;
    private String pattern;

    @Setup
    public void setup() {
        text = "This is a simulated large grid report. The text contains many characters. "
             + "We are trying to simulate a real scenario where a fault like OVERVOLTAGE might appear near the end. "
             + "However, before that, we have mostly nominal readings. "
             + "Zone 1: nominal. Zone 2: nominal. Zone 3: nominal. "
             + "Wait, suddenly there is an OVERVOLTAGE alert triggered by Substation 4.";
        pattern = "OVERVOLTAGE";
    }

    @Benchmark
    public void benchmarkKMP() {
        KMP.search(text, pattern);
    }

    @Benchmark
    public void benchmarkRabinKarp() {
        RabinKarp.search(text, pattern);
    }
}
