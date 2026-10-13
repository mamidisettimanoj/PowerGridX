# Architecture

## Data flow

1. At start-up `DataStore.load()` reads both CSV files with `DataIngestionService`.
2. Rows with missing readings are skipped and recorded in a `DataQualityReport` (counts, completeness, outage periods).
3. `GridEvent.detect()` scans the household readings for operating-limit violations.
4. The immutable result is published as one snapshot. `DashboardService` computes every dashboard figure from it.
5. The React frontend reads the figures through the REST API (`frontend/src/api.ts`).

## Operating limits

| Event | Rule | Severity |
|-------|------|----------|
| Under-voltage | voltage below 225 V | Warning |
| Over-voltage | voltage above 250 V | Critical |
| Overcurrent | current above 40 A | Warning |
| Load surge | active power above 8 kW | Notice |

## Units

* Zone demand readings are average kW over 10 minutes. Energy (MWh) = sum of kW × (10 / 60) / 1000.
* Household readings are average kW over 1 minute. Energy (kWh) = sum of kW / 60.

## Modules and algorithms

| Module | Algorithms |
|--------|-----------|
| Incident Intelligence | KMP, Rabin-Karp, Z-Function, Aho-Corasick over the incident log |
| Maintenance Reports | Suffix array (prefix doubling, O(N log² N)), LCP array (Kasai) |
| Maintenance Optimization | Levenshtein distance, bitmask dynamic programming (routing), optimal BST |
| Grid Capacity | Edmonds-Karp, Dinic |
| Crew Scheduling | Vertex cover: 2-approximation and exact search |
| Telemetry Processing | Reservoir sampling, ForkJoin parallel reduce, randomized quicksort |
