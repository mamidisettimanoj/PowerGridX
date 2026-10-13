package com.powergridx.algorithms.parallel;

import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

public class Parallel {
    
    private static final ForkJoinPool pool = new ForkJoinPool();

    static class SumTask extends RecursiveTask<Double> {
        private final double[] arr;
        private final int start;
        private final int end;
        private static final int THRESHOLD = 10000;

        SumTask(double[] arr, int start, int end) {
            this.arr = arr;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Double compute() {
            if (end - start <= THRESHOLD) {
                double sum = 0;
                for (int i = start; i < end; i++) {
                    sum += arr[i];
                }
                return sum;
            } else {
                int mid = start + (end - start) / 2;
                SumTask left = new SumTask(arr, start, mid);
                SumTask right = new SumTask(arr, mid, end);
                left.fork();
                double rightResult = right.compute();
                double leftResult = left.join();
                return leftResult + rightResult;
            }
        }
    }

    public static double parallelReduce(double[] arr) {
        return pool.invoke(new SumTask(arr, 0, arr.length));
    }

    public static double sequentialReduce(double[] arr) {
        double sum = 0;
        for (double v : arr) {
            sum += v;
        }
        return sum;
    }
}
