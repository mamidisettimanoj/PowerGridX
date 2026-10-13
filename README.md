# PowerGridX — Smart Energy Management Platform

PowerGridX is a web platform for monitoring and planning the operation of an electricity grid. It turns two real
energy datasets into live operational views: demand by zone, household load and voltage quality, grid incidents,
maintenance planning and reporting.

## Architecture

* **Backend**: Java 21 with the Javalin web framework. Both CSV files are loaded once at start-up into an in-memory
  snapshot; every dashboard figure is computed from that data.
* **Frontend**: React + TypeScript + Vite, dark industrial theme (plain CSS), charts with Recharts.
* **Algorithms**: the search, optimisation, flow and scheduling engines are implemented from scratch in
  `backend/src/main/java/com/powergridx/algorithms`.
* **Testing and performance**: JUnit 5 and JMH (Java Microbenchmark Harness).

## Data

| File | Content | Rows | Interval | Period |
|------|---------|------|----------|--------|
| `household_power_consumption.csv` | Household smart-meter readings (power, voltage, current, sub-metering) | 260,640 | 1 minute | 1 Jan – 30 Jun 2007 |
| `powerconsumption.csv` | Zone demand with weather (temperature, humidity, wind) | 52,416 | 10 minutes | 1 Jan – 30 Dec 2017 |

The two files come from different places and years, so the dashboard presents them side by side and never adds them
together. Readings that the meter did not record (`?` or blank) are left out and reported on the dashboard under
**Data Quality**. See [docs/dataset.md](docs/dataset.md).

## Platform modules

| Module | What it does | Technique |
|--------|--------------|-----------|
| Overview Dashboard | Energy, peak demand, household load, grid events, data quality | Streaming aggregation |
| Grid Analytics | Load by hour of day, daily voltage range, demand vs temperature | Time-series aggregation |
| Incident Intelligence | Browse operating-limit violations and search the incident log | KMP, Rabin-Karp, Z-Function, Aho-Corasick |
| Maintenance Reports | Generates the maintenance report and builds a searchable index | Suffix array, LCP array |
| Maintenance Optimization | Typo-tolerant equipment lookup, drone inspection routing | Edit distance, bitmask DP |
| Grid Capacity | Maximum deliverable power from generator to city | Edmonds-Karp, Dinic |
| Crew Scheduling | Minimum crews to cover conflicting substations | Vertex cover (approximate and exact) |
| Telemetry Processing | Whole-dataset aggregation and sampling | ForkJoin parallel reduce, reservoir sampling |
| Performance | JMH benchmark results | JMH |

## Setup

### Prerequisites
* Java 21
* Maven
* Node.js 18+

### 1. Start the backend
```bash
cd backend
mvn clean compile
mvn exec:java -Dexec.mainClass="com.powergridx.Main"
```
The API starts on `http://localhost:8080`. The server refuses to start if a CSV file is missing or contains no usable
rows. Check `http://localhost:8080/api/health` to see what was loaded.

Optional environment variables:

| Variable | Default | Purpose |
|----------|---------|---------|
| `PORT` | `8080` | API port |
| `POWERGRIDX_DATA_DIR` | project root | Folder containing the two CSV files |

### 2. Start the frontend
```bash
cd frontend
npm install
npm run dev
```
The app is available at `http://localhost:5173`. To point it at a different API, copy `.env.example` to `.env` and set
`VITE_API_URL`.

### 3. Run benchmarks
```bash
cd backend
mvn clean package
java -jar target/benchmarks.jar -rf json -rff benchmark-results.json
```

## API overview

| Endpoint | Description |
|----------|-------------|
| `GET /api/health` | Dataset status |
| `GET /api/dashboard/overview` | KPIs, event counts, data-quality summary |
| `GET /api/dashboard/zones/series?granularity=day\|week\|month` | Zone energy over time (MWh) |
| `GET /api/dashboard/household/load-profile` | Average load by hour, weekday vs weekend |
| `GET /api/dashboard/household/voltage-trend` | Daily minimum / average / maximum voltage |
| `GET /api/faults/events?type=&severity=&limit=&offset=` | Grid events, newest first |
| `GET /api/faults/search?q=&algo=` | Search the incident log |
| `GET /api/reports/suffix` | Maintenance report with suffix index |
| `GET /api/network/flow?algo=` | Grid capacity |
| `GET /api/np/vertex-cover` | Crew scheduling |
| `GET /api/parallel/process` | Telemetry processing |
| `GET /api/benchmarks` | Benchmark results |
