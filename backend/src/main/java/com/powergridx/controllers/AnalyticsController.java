package com.powergridx.controllers;

import com.powergridx.services.DashboardService;
import io.javalin.http.Context;

import java.util.Map;

/** Dashboard endpoints. Every figure is computed from the loaded CSV data. */
public class AnalyticsController {

    /** KPIs, event counts and the data-quality summary for both datasets. */
    public static void getOverview(Context ctx) {
        ctx.json(DashboardService.overview());
    }

    /** Zone energy over time. ?granularity=day|week|month (default day). */
    public static void getZoneSeries(Context ctx) {
        String granularity = ctx.queryParam("granularity");
        if (granularity == null || granularity.isBlank()) granularity = "day";
        try {
            ctx.json(DashboardService.zoneSeries(granularity.toLowerCase()));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /** Average household load by hour of day (weekday vs weekend). */
    public static void getLoadProfile(Context ctx) {
        ctx.json(DashboardService.loadProfile());
    }

    /** Daily min / average / max voltage and daily energy for the household meter. */
    public static void getVoltageTrend(Context ctx) {
        ctx.json(DashboardService.voltageTrend());
    }
}
