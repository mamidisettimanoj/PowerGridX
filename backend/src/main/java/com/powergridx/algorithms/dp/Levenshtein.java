package com.powergridx.algorithms.dp;

public class Levenshtein {
    public static int distance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + cost, // substitution
                        Math.min(
                            dp[i - 1][j] + 1,    // deletion
                            dp[i][j - 1] + 1     // insertion
                        )
                    );
                }
            }
        }
        return dp[a.length()][b.length()];
    }
}
