package com.powergridx.algorithms.strings;

import java.util.ArrayList;
import java.util.List;

public class ZFunction {
    /** Z-array of s: z[i] = length of the longest common prefix of s and s[i..]; z[0] = s.length(). */
    static int[] zArray(String s) {
        int n = s.length();
        int[] z = new int[n];
        if (n == 0) return z;
        z[0] = n;
        int l = 0, r = 0;
        for (int i = 1; i < n; i++) {
            if (i < r) z[i] = Math.min(r - i, z[i - l]);
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
            if (i + z[i] > r) { l = i; r = i + z[i]; }
        }
        return z;
    }

    /**
     * Finds every occurrence of pattern in text using the Z-algorithm. The pattern's Z-array is reused while
     * scanning the text, so no separator character is needed and any character may appear in either string.
     */
    public static List<Integer> search(String text, String pattern) {
        List<Integer> matches = new ArrayList<>();
        int m = pattern.length(), n = text.length();
        if (m == 0 || n < m) return matches;

        int[] zp = zArray(pattern);
        int l = 0, r = 0; // text[l..r) is known to equal pattern[0..r-l)
        for (int i = 0; i < n; i++) {
            int k = 0;
            if (i < r) k = Math.min(r - i, zp[i - l]);
            while (k < m && i + k < n && text.charAt(i + k) == pattern.charAt(k)) k++;
            if (i + k > r) { l = i; r = i + k; }
            if (k == m) matches.add(i);
        }
        return matches;
    }
}
