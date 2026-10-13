package com.powergridx.models;

import java.time.LocalDateTime;

/** One 10-minute reading of zone demand together with the weather at that time. */
public class ZoneRecord {
    public LocalDateTime timestamp;
    public double temperature;          // degrees C
    public double humidity;             // %
    public double windSpeed;            // m/s
    public double generalDiffuseFlows;
    public double diffuseFlows;
    public double powerConsumptionZone1; // kW (average over the interval)
    public double powerConsumptionZone2; // kW
    public double powerConsumptionZone3; // kW

    public ZoneRecord() {}
}
