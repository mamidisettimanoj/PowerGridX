export interface Health {
  status: string;
  householdRows: number;
  zoneRows: number;
  gridEvents: number;
  householdTo: string;
  zonesTo: string;
  loadedAt: string;
}

export interface Gap {
  from: string;
  to: string;
  rows: number;
  minutes: number;
}

export interface DataQuality {
  dataset: string;
  file: string;
  intervalMinutes: number;
  totalRows: number;
  loadedRows: number;
  skippedRows: number;
  missingValueRows: number;
  malformedRows: number;
  completenessPct: number;
  from: string;
  to: string;
  gapCount: number;
  longestGaps: Gap[];
}

export interface Overview {
  status: string;
  zones: {
    energyMWh: { zone1: number; zone2: number; zone3: number; total: number };
    avgDemandKw: number;
    peakDemandKw: number;
    peakDemandAt: string;
    avgTemperatureC: number;
    minTemperatureC: number;
    maxTemperatureC: number;
    avgHumidityPct: number;
    avgWindSpeed: number;
  };
  household: {
    avgLoadKw: number;
    peakLoadKw: number;
    peakLoadAt: string;
    energyKwh: number;
    avgVoltage: number;
    minVoltage: number;
    maxVoltage: number;
  };
  events: {
    total: number;
    byType: Record<EventType, number>;
    bySeverity: Record<Severity, number>;
    thresholds: { underVoltageV: number; overVoltageV: number; overcurrentA: number; loadSurgeKw: number };
  };
  dataQuality: { household: DataQuality; zones: DataQuality };
}

export interface ZonePoint {
  date: string;
  zone1: number;
  zone2: number;
  zone3: number;
  total: number;
  avgTemperatureC: number;
  avgHumidityPct: number;
  samples: number;
}

export interface ZoneSeries {
  granularity: Granularity;
  unit: string;
  points: ZonePoint[];
}

export type Granularity = 'day' | 'week' | 'month';

export interface LoadProfile {
  peakHour: number;
  peakAvgLoadKw: number;
  points: { hour: number; label: string; avgLoadKw: number; weekdayKw: number; weekendKw: number; avgVoltage: number }[];
}

export interface VoltageTrend {
  underVoltageLimitV: number;
  overVoltageLimitV: number;
  points: { date: string; minVoltage: number; avgVoltage: number; maxVoltage: number; energyKwh: number; samples: number }[];
}

export type EventType = 'OVER_VOLTAGE' | 'UNDER_VOLTAGE' | 'OVERCURRENT' | 'LOAD_SURGE';
export type Severity = 'CRITICAL' | 'WARNING' | 'NOTICE';

export interface GridEvent {
  timestamp: string;
  type: EventType;
  severity: Severity;
  value: number;
  unit: string;
  limit: number;
  message: string;
  line?: number;
}

export interface EventPage {
  total: number;
  offset: number;
  limit: number;
  events: GridEvent[];
}

export interface SearchResult {
  algorithm: string;
  query: string;
  logLines: number;
  occurrences: number;
  matchedLines: number;
  truncated: boolean;
  timeMicros: number;
  results: GridEvent[];
}
