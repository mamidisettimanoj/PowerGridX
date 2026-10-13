import { Droplets, Thermometer, TrendingDown, TrendingUp, Zap } from 'lucide-react';
import { Area, CartesianGrid, ComposedChart, Legend, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useApi } from '../api';
import type { LoadProfile, Overview, VoltageTrend, ZoneSeries } from '../types';
import { EVENT_LABELS, fmtAxisDate, fmtDate, fmtNumber } from '../format';
import { CHART_TOOLTIP, ErrorCard, KpiCard, LoadingBlock, PageHeader } from '../components/ui';

const GridAnalytics = () => {
  const overview = useApi<Overview>('/api/dashboard/overview');
  const profile = useApi<LoadProfile>('/api/dashboard/household/load-profile');
  const voltage = useApi<VoltageTrend>('/api/dashboard/household/voltage-trend');
  const daily = useApi<ZoneSeries>('/api/dashboard/zones/series?granularity=day');

  if (overview.error) {
    return (
      <div>
        <PageHeader icon={<Zap size={32} style={{ color: 'var(--status-warning)' }} />} title="Grid Analytics" />
        <ErrorCard message={overview.error} onRetry={overview.reload} />
      </div>
    );
  }

  const o = overview.data;
  const t = o?.events.thresholds;
  const limits = t
    ? [
        { type: 'UNDER_VOLTAGE', rule: `below ${t.underVoltageV} V`, color: 'var(--status-warning)' },
        { type: 'OVER_VOLTAGE', rule: `above ${t.overVoltageV} V`, color: 'var(--status-danger)' },
        { type: 'OVERCURRENT', rule: `above ${t.overcurrentA} A`, color: 'var(--accent-blue)' },
        { type: 'LOAD_SURGE', rule: `above ${t.loadSurgeKw} kW`, color: 'var(--accent-cyan)' },
      ]
    : [];

  return (
    <div>
      <PageHeader
        icon={<Zap size={32} style={{ color: 'var(--status-warning)' }} />}
        title="Grid Analytics"
        subtitle={o ? `Analysis of ${fmtNumber(o.dataQuality.household.loadedRows)} household meter readings and ${fmtNumber(o.dataQuality.zones.loadedRows)} zone readings.` : undefined}
      />

      <div className="grid grid-cols-4" style={{ marginBottom: '1.5rem' }}>
        <KpiCard
          label="Avg Temperature"
          value={o ? `${fmtNumber(o.zones.avgTemperatureC, 1)} °C` : '--'}
          sub={o ? `Range ${fmtNumber(o.zones.minTemperatureC, 1)} to ${fmtNumber(o.zones.maxTemperatureC, 1)} °C` : undefined}
          icon={<Thermometer size={20} style={{ color: 'var(--status-danger)' }} />}
        />
        <KpiCard
          label="Avg Humidity"
          value={o ? `${fmtNumber(o.zones.avgHumidityPct, 1)} %` : '--'}
          sub={o ? `Avg wind ${fmtNumber(o.zones.avgWindSpeed, 2)} m/s` : undefined}
          icon={<Droplets size={20} style={{ color: 'var(--accent-cyan)' }} />}
        />
        <KpiCard
          label="Min Voltage"
          value={o ? `${fmtNumber(o.household.minVoltage, 1)} V` : '--'}
          sub={o ? `Average ${fmtNumber(o.household.avgVoltage, 1)} V` : undefined}
          icon={<TrendingDown size={20} style={{ color: 'var(--status-warning)' }} />}
        />
        <KpiCard
          label="Max Voltage"
          value={o ? `${fmtNumber(o.household.maxVoltage, 1)} V` : '--'}
          sub="Household meter"
          icon={<TrendingUp size={20} style={{ color: 'var(--status-success)' }} />}
        />
      </div>

      <div className="grid grid-cols-4" style={{ marginBottom: '1.5rem' }}>
        {limits.map((l) => (
          <div key={l.type} className="card" style={{ borderLeft: `4px solid ${l.color}` }}>
            <div className="kpi-label">{EVENT_LABELS[l.type]}</div>
            <div className="kpi-value" style={{ color: l.color }}>
              {o ? fmtNumber(o.events.byType[l.type as keyof typeof o.events.byType]) : '--'}
            </div>
            <div className="muted" style={{ fontSize: '0.85rem' }}>
              Readings {l.rule}
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-2" style={{ marginBottom: '1.5rem' }}>
        <div className="card chart-card">
          <h3>Household Load by Hour of Day (kW)</h3>
          <p className="muted" style={{ fontSize: '0.85rem', marginBottom: '1rem' }}>
            {profile.data ? `Evening peak at ${String(profile.data.peakHour).padStart(2, '0')}:00 (average ${profile.data.peakAvgLoadKw} kW)` : 'Average across all days'}
          </p>
          {profile.error ? (
            <ErrorCard message={profile.error} onRetry={profile.reload} />
          ) : profile.data ? (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={profile.data.points}>
                <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                <XAxis dataKey="label" stroke="#888" fontSize={12} interval={2} />
                <YAxis stroke="#888" fontSize={12} />
                <Tooltip contentStyle={CHART_TOOLTIP} formatter={(v) => `${fmtNumber(Number(v), 2)} kW`} />
                <Legend />
                <Line type="monotone" dataKey="weekdayKw" name="Weekday" stroke="#06B6D4" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="weekendKw" name="Weekend" stroke="#F59E0B" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <LoadingBlock height={300} />
          )}
        </div>

        <div className="card chart-card">
          <h3>Daily Voltage Range (V)</h3>
          <p className="muted" style={{ fontSize: '0.85rem', marginBottom: '1rem' }}>Daily minimum, average and maximum against the operating limits</p>
          {voltage.error ? (
            <ErrorCard message={voltage.error} onRetry={voltage.reload} />
          ) : voltage.data ? (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={voltage.data.points}>
                <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                <XAxis dataKey="date" stroke="#888" fontSize={12} minTickGap={40} tickFormatter={(d: string) => fmtAxisDate(d)} />
                <YAxis stroke="#888" fontSize={12} domain={[220, 252]} />
                <Tooltip contentStyle={CHART_TOOLTIP} labelFormatter={(d) => fmtDate(String(d))} formatter={(v) => `${fmtNumber(Number(v), 1)} V`} />
                <Legend />
                <ReferenceLine y={voltage.data.overVoltageLimitV} stroke="#EF4444" strokeDasharray="5 5" label={{ value: `${voltage.data.overVoltageLimitV} V limit`, fill: '#EF4444', fontSize: 11, position: 'insideTopRight' }} />
                <ReferenceLine y={voltage.data.underVoltageLimitV} stroke="#F59E0B" strokeDasharray="5 5" label={{ value: `${voltage.data.underVoltageLimitV} V limit`, fill: '#F59E0B', fontSize: 11, position: 'insideBottomRight' }} />
                <Line type="monotone" dataKey="maxVoltage" name="Max" stroke="#EF4444" strokeWidth={1} dot={false} />
                <Line type="monotone" dataKey="avgVoltage" name="Average" stroke="#06B6D4" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="minVoltage" name="Min" stroke="#F59E0B" strokeWidth={1} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <LoadingBlock height={300} />
          )}
        </div>
      </div>

      <div className="card chart-card">
        <h3>Zone Demand vs Temperature</h3>
        <p className="muted" style={{ fontSize: '0.85rem', marginBottom: '1rem' }}>Daily energy across all zones (MWh) with the daily average temperature (°C)</p>
        {daily.error ? (
          <ErrorCard message={daily.error} onRetry={daily.reload} />
        ) : daily.data ? (
          <ResponsiveContainer width="100%" height={320}>
            <ComposedChart data={daily.data.points}>
              <CartesianGrid strokeDasharray="3 3" stroke="#333" />
              <XAxis dataKey="date" stroke="#888" fontSize={12} minTickGap={50} tickFormatter={(d: string) => fmtAxisDate(d)} />
              <YAxis yAxisId="energy" stroke="#888" fontSize={12} tickFormatter={(v: number) => fmtNumber(v)} />
              <YAxis yAxisId="temp" orientation="right" stroke="#888" fontSize={12} unit=" °C" />
              <Tooltip contentStyle={CHART_TOOLTIP} labelFormatter={(d) => fmtDate(String(d))} />
              <Legend />
              <Area yAxisId="energy" type="monotone" dataKey="total" name="Energy (MWh)" stroke="#06B6D4" fill="#06B6D4" fillOpacity={0.2} />
              <Line yAxisId="temp" type="monotone" dataKey="avgTemperatureC" name="Temperature (°C)" stroke="#F59E0B" strokeWidth={2} dot={false} />
            </ComposedChart>
          </ResponsiveContainer>
        ) : (
          <LoadingBlock height={320} />
        )}
      </div>
    </div>
  );
};

export default GridAnalytics;
