package com.powergridx.controllers;

import io.javalin.http.Context;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import com.powergridx.algorithms.strings.KMP;
import com.powergridx.algorithms.strings.RabinKarp;
import com.powergridx.algorithms.strings.ZFunction;
import com.powergridx.algorithms.strings.AhoCorasick;

public class DSAController {
    
    // Request format: { "text": "...", "pattern": "..." }
    public static void searchKMP(Context ctx) {
        String text = ctx.queryParam("text");
        String pattern = ctx.queryParam("pattern");
        if (text == null || pattern == null) {
            ctx.status(400).json(Map.of("error", "Missing text or pattern"));
            return;
        }
        
        long start = System.nanoTime();
        List<Integer> matches = KMP.search(text, pattern);
        long end = System.nanoTime();
        
        ctx.json(Map.of(
            "algorithm", "KMP",
            "matches", matches,
            "timeNs", (end - start)
        ));
    }

    public static void searchRabinKarp(Context ctx) {
        String text = ctx.queryParam("text");
        String pattern = ctx.queryParam("pattern");
        if (text == null || pattern == null) {
            ctx.status(400).json(Map.of("error", "Missing text or pattern"));
            return;
        }
        
        long start = System.nanoTime();
        List<Integer> matches = RabinKarp.search(text, pattern);
        long end = System.nanoTime();
        
        ctx.json(Map.of(
            "algorithm", "Rabin-Karp",
            "matches", matches,
            "timeNs", (end - start)
        ));
    }

    public static void searchZFunction(Context ctx) {
        String text = ctx.queryParam("text");
        String pattern = ctx.queryParam("pattern");
        if (text == null || pattern == null) {
            ctx.status(400).json(Map.of("error", "Missing text or pattern"));
            return;
        }
        
        long start = System.nanoTime();
        List<Integer> matches = ZFunction.search(text, pattern);
        long end = System.nanoTime();
        
        ctx.json(Map.of(
            "algorithm", "Z-Function",
            "matches", matches,
            "timeNs", (end - start)
        ));
    }

    public static void searchAhoCorasick(Context ctx) {
        String text = ctx.queryParam("text");
        String patternsParam = ctx.queryParam("patterns"); // comma separated
        if (text == null || patternsParam == null) {
            ctx.status(400).json(Map.of("error", "Missing text or patterns"));
            return;
        }
        
        List<String> patterns = Arrays.asList(patternsParam.split(","));
        AhoCorasick ac = new AhoCorasick(patterns);
        
        long start = System.nanoTime();
        List<AhoCorasick.Match> matches = ac.search(text);
        long end = System.nanoTime();
        
        ctx.json(Map.of(
            "algorithm", "Aho-Corasick",
            "matches", matches,
            "timeNs", (end - start)
        ));
    }
}
