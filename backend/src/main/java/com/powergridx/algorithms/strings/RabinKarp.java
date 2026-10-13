package com.powergridx.algorithms.strings;

import java.util.ArrayList;
import java.util.List;

public class RabinKarp {
    public static final int d = 256;
    public static final int q = 101; // A prime number

    public static List<Integer> search(String text, String pattern) {
        List<Integer> matches = new ArrayList<>();
        int M = pattern.length();
        int N = text.length();
        if (M == 0 || N < M) return matches;

        int i, j;
        int p = 0; // hash value for pattern
        int t = 0; // hash value for txt
        int h = 1;

        for (i = 0; i < M - 1; i++) {
            h = (h * d) % q;
        }

        for (i = 0; i < M; i++) {
            p = (d * p + pattern.charAt(i)) % q;
            t = (d * t + text.charAt(i)) % q;
        }

        for (i = 0; i <= N - M; i++) {
            if (p == t) {
                for (j = 0; j < M; j++) {
                    if (text.charAt(i + j) != pattern.charAt(j)) {
                        break;
                    }
                }
                if (j == M) {
                    matches.add(i);
                }
            }

            if (i < N - M) {
                t = (d * (t - text.charAt(i) * h) + text.charAt(i + M)) % q;
                if (t < 0) {
                    t = (t + q);
                }
            }
        }
        return matches;
    }
}
