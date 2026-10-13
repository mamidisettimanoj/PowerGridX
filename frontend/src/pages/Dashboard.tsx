import { useState } from 'react';
import { Activity, AlertTriangle, Gauge, Zap } from 'lucide-react';
import { Area, AreaChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useApi } from '../api';
import type { EventPage, Granularity, Overview, ZoneSeries } from '../types';
import { EVENT_LABELS, fmtAxisDate, fmtDate, fmtDateTime, fmtEnergyMWh, fmtMinutes, fmtNumber } from '../format';
import { CHART_TOOLTIP, ErrorCard, KpiCard, LoadingBlock, PageHeader, Segmented, SeverityBadge } from '../components/ui';

const ZONE_COLORS = { zone1: '#06B6D4', zone2: '#F59E0B', zone3: '#3B82F6' };

const Dashboard = () => {
  const [granularity, setGranularity] = useState<Granularity>('week');
  const overview = useApi<Overview>('/api/dashboard/overview');
  const series = useApi<ZoneSeries>(`/api/dashboard/zones/series?granularity=${granularity}`);
  const events = useApi<EventPage>('/api/faults/events?limit=8');

  if (overview.error) {
    return (
      <div>
        <PageHeader icon={<Activity size={32} style={{ color: 'var(--accent-cyan)' }} />} title="Overview Dashboard" />
        <ErrorCard message={overview.error} onRetry={overview.reload} />
      </div>
    );
  }

  const o = overview.data;
  const share = o
    ? [
        { name: 'Zone 1', value: o.zones.energyMWh.zone1, color: ZONE_COLORS.zone1 },
        { name: 'Zone 2', value: o.zones.energyMWh.zone2, color: ZONE_COLORS.zone2 },
        { name: 'Zone 3', value: o.zones.energyMWh.zone3, color: ZONE_COLORS.zone3 },
      ]
    : [];
  const zq = o?.dataQuality.zones;
  const hq = o?.dataQuality.household;

  return (
    <div>
      <PageHeader
        icon={<Activity size={32} style={{ color: 'var(--accent-cyan)' }} />}
        title="Overview Dashboard"
        subtitle={o ? `Zone demand ${fmtDate(zq?.from)} – ${fmtDate(zq?.to)} · Household meter ${fmtDate(hq?.from)} – ${fmtDate(hq?.to)}` : undefined}
      />

      <div className="grid grid-cols-4" style={{ marginBottom: '1.5rem' }}>
        <KpiCard
          label="Zone Energy"
          value={o ? fmtEnergyMWh(o.zones.energyMWh.total) : '--'}
          sub={o ? `Avg demand ${fmtNumber(o.zones.avgDemandKw / 1000, 1)} MW` : undefined}
          icon={<Activity size={20} style={{ color: 'var(--accent-cyan)' }} />}
          accent="var(--accent-cyan)"
        />
        <KpiCard
          label="Peak Zone Demand"
          value={o ? `${fmtNumber(o.zones.peakDemandKw / 1000, 1)} MW` : '--'}
          sub={o ? fmtDateTime(o.zones.peakDemandAt) : undefined}
          icon={<Gauge size={20} style={{ color: 'var(--accent-blue)' }} />}
          accent="var(--accent-blue)"
        />
        <KpiCard
          label="Avg Household Load"
          value={o ? `${fmtNumber(o.household.avgLoadKw, 2)} kW` : '--'}
          sub={o ? `Peak ${fmtNumber(o.household.peakLoadKw, 2)} kW · ${fmtDate(o.household.peakLoadAt)}` : undefined}
          icon={<Zap size={20} style={{ color: 'var(--status-warning)' }} />}
          accent="var(--status-warning)"
        />
        <KpiCard
          label="Grid Events"
          value={o ? fmtNumber(o.events.total) : '--'}
          sub={o ? `${o.events.bySeverity.CRITICAL} critical · ${o.events.bySeverity.WARNING} warning · ${o.events.bySeverity.NOTICE} notice` : undefined}
          icon={<AlertTriangle size={20} style={{ color: 'var(--status-danger)' }} />}
          accent="var(--status-danger)"
        />
      </div>

      <div className="grid" style={{ gridTemplateColumns: '2fr 1fr', marginBottom: '1.5rem' }}>
        <div className="card chart-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3>Zone Energy Over Time (MWh)</h3>
            <Segmented
              value={granularity}
              onChange={setGranularity}
              options={[
                { value: 'day', label: 'Daily' },
                { value: 'week', label: 'Weekly' },
                { value: 'month', label: 'Monthly' },
              ]}
            />
          </div>
          {series.error ? (
            <ErrorCard message={series.error} onRetry={series.reload} />
          ) : series.data ? (
            <ResponsiveContainer width="100%" height={320}>
              <AreaChart data={series.data.points}>
                <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                <XAxis dataKey="date" stroke="#888" fontSize={12} minTickGap={40} tickFormatter={(d: string) => fmtAxisDate(d, granularity)} />
                <YAxis stroke="#888" fontSize={12} tickFormatter={(v: number) => fmtNumber(v)} />
                <Tooltip contentStyle={CHART_TOOLTIP} labelFormatter={(d) => fmtDate(String(d))} formatter={(v) => `${fmtNumber(Number(v), 1)} MWh`} />
                <Legend />
                <Area type="monotone" stackId="1" dataKey="zone1" name="Zone 1" stroke={ZONE_COLORS.zone1} fill={ZONE_COLORS.zone1} fillOpacity={0.35} />
                <Area type="monotone" stackId="1" dataKey="zone2" name="Zone 2" stroke={ZONE_COLORS.zone2} fill={ZONE_COLORS.zone2} fillOpacity={0.35} />
                <Area type="monotone" stackId="1" dataKey="zone3" name="Zone 3" stroke={ZONE_COLORS.zone3} fill={ZONE_COLORS.zone3} fillOpacity={0.35} />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <LoadingBlock height={320} label="Loading zone energy..." />
          )}
        </div>

        <div className="card chart-card">
          <h3 style={{ marginBottom: '1rem' }}>Energy Share by Zone</h3>
          {o ? (
            <ResponsiveContainer width="100%" height={320}>
              <PieChart>
                <Pie data={share} dataKey="value" nameKey="name" innerRadius={65} outerRadius={105} paddingAngle={3} label={({ percent }) => `${((percent ?? 0) * 100).toFixed(1)}%`}>
                  {share.map((s) => (
                    <Cell key={s.name} fill={s.color} stroke="none" />
                  ))}
                </Pie>
                <Tooltip contentStyle={CHART_TOOLTIP} formatter={(v) => fmtEnergyMWh(Number(v))} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <LoadingBlock height={320} />
          )}
        </div>
      </div>

      <div className="grid" style={{ gridTemplateColumns: '2fr 1fr' }}>
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>Recent Grid Events</h3>
          {events.error ? (
            <ErrorCard message={events.error} onRetry={events.reload} />
          ) : events.data ? (
            <table className="table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Event</th>
                  <th>Severity</th>
                  <th>Detail</th>
                </tr>
              </thead>
              <tbody>
                {events.data.events.map((e, i) => (
                  <tr key={i}>
                    <td className="nowrap">{fmtDateTime(e.timestamp)}</td>
                    <td>{EVENT_LABELS[e.type] ?? e.type}</td>
                    <td>
                      <SeverityBadge severity={e.severity} />
                    </td>
                    <td className="muted">{e.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <LoadingBlock height={260} />
          )}
        </div>

        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>Data Quality</h3>
          {o && hq && zq ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              {[hq, zq].map((q) => (
                <div key={q.dataset}>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <strong>{q.dataset === 'household' ? 'Household meter' : 'Zone demand'}</strong>
                    <span style={{ color: q.completenessPct >= 99.5 ? 'var(--status-success)' : 'var(--status-warning)' }}>{q.completenessPct.toFixed(2)}% complete</span>
                  </div>
                  <div className="bar">
                    <div style={{ width: `${q.completenessPct}%` }} />
                  </div>
                  <p className="muted" style={{ fontSize: '0.8rem' }}>
                    {fmtNumber(q.loadedRows)} of {fmtNumber(q.totalRows)} readings used
                    {q.skippedRows > 0 && ` · ${fmtNumber(q.skippedRows)} missing`}
                  </p>
                  {q.longestGaps[0] && (
                    <p className="muted" style={{ fontSize: '0.8rem' }}>
                      Longest outage: {fmtMinutes(q.longestGaps[0].minutes)} from {fmtDateTime(q.longestGaps[0].from)}
                    </p>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <LoadingBlock height={260} />
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
