package com.powergridx.algorithms.suffix;

public class LCP {
    public static int[] kasai(String text, int[] sa) {
        int n = sa.length;
        int[] lcp = new int[n];
        int[] invSuff = new int[n];

        for (int i = 0; i < n; i++) {
            invSuff[sa[i]] = i;
        }

        int k = 0;
        for (int i = 0; i < n; i++) {
            if (invSuff[i] == n - 1) {
                k = 0;
                continue;
            }
            int j = sa[invSuff[i] + 1];
            while (i + k < n && j + k < n && text.charAt(i + k) == text.charAt(j + k)) {
                k++;
            }
            lcp[invSuff[i]] = k;
            if (k > 0) {
                k--;
            }
        }
        return lcp;
    }
}
