package com.powergridx.algorithms.suffix;

import java.util.Arrays;
import java.util.Comparator;

public class SuffixArray {
    public static class Suffix {
        public int index;
        public int[] rank = new int[2];
    }

    public static int[] buildSuffixArray(String text) {
        int n = text.length();
        Suffix[] suffixes = new Suffix[n];
        
        for (int i = 0; i < n; i++) {
            suffixes[i] = new Suffix();
            suffixes[i].index = i;
            suffixes[i].rank[0] = text.charAt(i); // Full ASCII support
            suffixes[i].rank[1] = ((i + 1) < n) ? text.charAt(i + 1) : -1;
        }

        Arrays.sort(suffixes, new Comparator<Suffix>() {
            public int compare(Suffix a, Suffix b) {
                return (a.rank[0] == b.rank[0]) ? 
                       (a.rank[1] - b.rank[1]) : 
                       (a.rank[0] - b.rank[0]);
            }
        });

        int[] ind = new int[n];
        for (int k = 4; k < 2 * n; k = k * 2) {
            int rank = 0;
            int prev_rank = suffixes[0].rank[0];
            suffixes[0].rank[0] = rank;
            ind[suffixes[0].index] = 0;

            for (int i = 1; i < n; i++) {
                if (suffixes[i].rank[0] == prev_rank &&
                    suffixes[i].rank[1] == suffixes[i - 1].rank[1]) {
                    prev_rank = suffixes[i].rank[0];
                    suffixes[i].rank[0] = rank;
                } else {
                    prev_rank = suffixes[i].rank[0];
                    suffixes[i].rank[0] = ++rank;
                }
                ind[suffixes[i].index] = i;
            }

            for (int i = 0; i < n; i++) {
                int nextindex = suffixes[i].index + k / 2;
                suffixes[i].rank[1] = (nextindex < n) ? 
                                      suffixes[ind[nextindex]].rank[0] : -1;
            }

            Arrays.sort(suffixes, new Comparator<Suffix>() {
                public int compare(Suffix a, Suffix b) {
                    return (a.rank[0] == b.rank[0]) ? 
                           (a.rank[1] - b.rank[1]) : 
                           (a.rank[0] - b.rank[0]);
                }
            });
        }

        int[] sa = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = suffixes[i].index;
        }
        return sa;
    }
}
