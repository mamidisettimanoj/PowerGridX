package com.powergridx.models;

import java.time.LocalDateTime;

/**
 * One minute-level reading from a household smart meter.
 * Only complete rows are kept; rows with missing sensor values are counted in the data-quality report.
 */
public class HouseholdRecord {
    public LocalDateTime timestamp;
    public double globalActivePower;    // kW
    public double globalReactivePower;  // kVAR
    public double voltage;              // V
    public double globalIntensity;      // A
    public double subMetering1;         // Wh (kitchen)
    public double subMetering2;         // Wh (laundry)
    public double subMetering3;         // Wh (water heater / A-C)

    public HouseholdRecord() {}
}
