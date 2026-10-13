package com.powergridx.algorithms.parallel;

import java.util.Random;

public class Randomized {
    
    private static final Random rand = new Random();

    /**
     * Randomized quicksort with three-way partitioning, so runs of equal values (common in meter data)
     * cost O(n) instead of O(n^2). Recurses into the smaller side only, keeping stack depth O(log n).
     */
    public static void quickSort(double[] arr, int low, int high) {
        while (low < high) {
            double pivot = arr[low + rand.nextInt(high - low + 1)];
            int lt = low, gt = high, i = low;
            while (i <= gt) {
                if (arr[i] < pivot) swap(arr, lt++, i++);
                else if (arr[i] > pivot) swap(arr, i, gt--);
                else i++;
            }
            if (lt - low < high - gt) {
                quickSort(arr, low, lt - 1);
                low = gt + 1;
            } else {
                quickSort(arr, gt + 1, high);
                high = lt - 1;
            }
        }
    }

    private static void swap(double[] arr, int i, int j) {
        double temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    public static double[] reservoirSampling(double[] stream, int k) {
        double[] reservoir = new double[Math.max(0, Math.min(k, stream.length))];
        for (int i = 0; i < reservoir.length; i++) {
            reservoir[i] = stream[i];
        }

        for (int i = reservoir.length; i < stream.length; i++) {
            if (reservoir.length == 0) break;
            int j = rand.nextInt(i + 1);
            if (j < reservoir.length) {
                reservoir[j] = stream[i];
            }
        }
        return reservoir;
    }
}
