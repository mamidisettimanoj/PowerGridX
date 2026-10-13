# Dataset Information

PowerGridX uses two real-world datasets. They describe different places and years and are never combined.

## 1. household_power_consumption.csv
* **Size**: ~13.9 MB, 260,640 rows (one per minute)
* **Period**: 1 Jan 2007 – 30 Jun 2007
* **Columns**: `index`, `Date`, `Time`, `Global_active_power` (kW), `Global_reactive_power` (kVAR), `Voltage` (V), `Global_intensity` (A), `Sub_metering_1..3` (Wh)
* **Date format**: day/month/year, with 2-digit (`1/1/07`) and 4-digit (`14/1/2007`) years mixed in the same file
* **Missing readings**: 3,771 rows (1.45%) contain `?` instead of numbers, in 13 separate outages. The longest runs from 28 Apr 2007 00:21 to 30 Apr 2007 14:23 (3,723 minutes).
* **Handling**: these rows are skipped, counted, and shown on the dashboard (Data Quality). 256,869 rows (98.55%) are used.

## 2. powerconsumption.csv
* **Size**: ~4.3 MB, 52,416 rows (one per 10 minutes)
* **Period**: 1 Jan 2017 – 30 Dec 2017
* **Columns**: `Datetime`, `Temperature`, `Humidity`, `WindSpeed`, `GeneralDiffuseFlows`, `DiffuseFlows`, `PowerConsumption_Zone1..3` (kW)
* **Date format**: month/day/year with 24-hour time
* **Missing readings**: none (100% complete)

## Loading rules
* A row is used only if its timestamp parses and every numeric column holds a finite number.
* Files are located automatically from the working directory (or `POWERGRIDX_DATA_DIR`).
* The server stops with an error if a file is missing or contains no usable rows.
