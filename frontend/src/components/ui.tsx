import type { ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

export function PageHeader({ icon, title, subtitle }: { icon: ReactNode; title: string; subtitle?: string }) {
  return (
    <div style={{ marginBottom: '1.75rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        {icon}
        <h1 style={{ margin: 0 }}>{title}</h1>
      </div>
      {subtitle && <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>{subtitle}</p>}
    </div>
  );
}

export function ErrorCard({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="card" style={{ borderLeft: '4px solid var(--status-danger)', display: 'flex', gap: '1rem', alignItems: 'center', justifyContent: 'space-between' }}>
      <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
        <AlertTriangle size={22} style={{ color: 'var(--status-danger)', flexShrink: 0 }} />
        <div>
          <strong>Could not load data</strong>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{message}</p>
        </div>
      </div>
      {onRetry && (
        <button className="btn btn-outline" onClick={onRetry}>
          <RefreshCw size={16} /> Retry
        </button>
      )}
    </div>
  );
}

export function LoadingBlock({ label = 'Loading...', height = 220 }: { label?: string; height?: number }) {
  return (
    <div className="skeleton" style={{ height, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
      {label}
    </div>
  );
}

export function KpiCard({ label, value, sub, icon, accent }: { label: string; value: string; sub?: ReactNode; icon?: ReactNode; accent?: string }) {
  return (
    <div className="card" style={accent ? { borderTop: `3px solid ${accent}` } : undefined}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div className="kpi-label">{label}</div>
        {icon}
      </div>
      <div className="kpi-value">{value}</div>
      {sub && <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>{sub}</div>}
    </div>
  );
}

export function Segmented<T extends string>({ value, options, onChange }: { value: T; options: { value: T; label: string }[]; onChange: (v: T) => void }) {
  return (
    <div className="segmented" role="group">
      {options.map((o) => (
        <button key={o.value} className={o.value === value ? 'active' : ''} onClick={() => onChange(o.value)}>
          {o.label}
        </button>
      ))}
    </div>
  );
}

const SEVERITY_COLOR: Record<string, string> = {
  CRITICAL: 'var(--status-danger)',
  WARNING: 'var(--status-warning)',
  NOTICE: 'var(--accent-blue)',
};

export function SeverityBadge({ severity }: { severity: string }) {
  const color = SEVERITY_COLOR[severity] ?? 'var(--text-secondary)';
  return (
    <span className="badge" style={{ color, borderColor: color }}>
      {severity.charAt(0) + severity.slice(1).toLowerCase()}
    </span>
  );
}

export const CHART_TOOLTIP = { backgroundColor: '#1a1d24', border: '1px solid #333', borderRadius: 8 } as const;
