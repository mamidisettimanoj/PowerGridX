import { useCallback, useEffect, useState } from 'react';
import type { KeyboardEvent, ReactNode } from 'react';
import { Search, ShieldAlert } from 'lucide-react';
import { apiGet, useApi } from '../api';
import type { EventPage, EventType, GridEvent, Overview, SearchResult } from '../types';
import { EVENT_LABELS, fmtDateTime, fmtNumber } from '../format';
import { ErrorCard, KpiCard, LoadingBlock, PageHeader, SeverityBadge } from '../components/ui';

const PAGE_SIZE = 15;

const TYPE_FILTERS: { value: 'ALL' | EventType; label: string }[] = [
  { value: 'ALL', label: 'All events' },
  { value: 'OVER_VOLTAGE', label: 'Over-voltage' },
  { value: 'UNDER_VOLTAGE', label: 'Under-voltage' },
  { value: 'OVERCURRENT', label: 'Overcurrent' },
  { value: 'LOAD_SURGE', label: 'Load surge' },
];

const ALGORITHMS = [
  { value: 'kmp', label: 'KMP' },
  { value: 'rabin-karp', label: 'Rabin-Karp' },
  { value: 'z-function', label: 'Z-Function' },
  { value: 'aho-corasick', label: 'Aho-Corasick (several terms, comma separated)' },
];

/** The same single-line form the server searches, so highlighted text lines up with what matched. */
function logLine(e: GridEvent): string {
  return `${e.timestamp.slice(0, 10)} ${e.timestamp.slice(11, 16)} | ${e.type} | ${e.severity} | ${e.message}`;
}

function highlight(text: string, query: string): ReactNode {
  const terms = query
    .split(',')
    .map((t) => t.trim().toLowerCase())
    .filter(Boolean);
  if (terms.length === 0) return text;
  const lower = text.toLowerCase();
  const parts: ReactNode[] = [];
  let i = 0;
  while (i < text.length) {
    let hit = '';
    for (const t of terms) {
      if (lower.startsWith(t, i) && t.length > hit.length) hit = t;
    }
    if (hit) {
      parts.push(<mark key={i}>{text.slice(i, i + hit.length)}</mark>);
      i += hit.length;
    } else {
      let j = i + 1;
      while (j < text.length && !terms.some((t) => lower.startsWith(t, j))) j++;
      parts.push(text.slice(i, j));
      i = j;
    }
  }
  return parts;
}

const Faults = () => {
  const overview = useApi<Overview>('/api/dashboard/overview');

  // Event browser
  const [type, setType] = useState<'ALL' | EventType>('ALL');
  const [offset, setOffset] = useState(0);
  const events = useApi<EventPage>(`/api/faults/events?type=${type}&limit=${PAGE_SIZE}&offset=${offset}`);

  // Log search
  const [query, setQuery] = useState('over_voltage');
  const [algo, setAlgo] = useState('kmp');
  const [result, setResult] = useState<SearchResult | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [searching, setSearching] = useState(false);

  const runSearch = useCallback(async () => {
    if (!query.trim()) {
      setSearchError('Enter a search term');
      return;
    }
    setSearching(true);
    setSearchError(null);
    try {
      setResult(await apiGet<SearchResult>(`/api/faults/search?q=${encodeURIComponent(query)}&algo=${algo}`));
    } catch (e) {
      setResult(null);
      setSearchError(e instanceof Error ? e.message : 'Search failed');
    } finally {
      setSearching(false);
    }
  }, [query, algo]);

  useEffect(() => {
    void runSearch();
    // run once on first load so the panel starts with real results
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') void runSearch();
  };

  const o = overview.data;
  const total = events.data?.total ?? 0;

  return (
    <div>
      <PageHeader
        icon={<ShieldAlert size={32} style={{ color: 'var(--status-danger)' }} />}
        title="Incident Intelligence"
        subtitle="Operating-limit violations detected in the household meter data, with full-text search across the incident log."
      />

      <div className="grid grid-cols-4" style={{ marginBottom: '1.5rem' }}>
        <KpiCard label="Total Events" value={o ? fmtNumber(o.events.total) : '--'} sub="All severities" accent="var(--accent-cyan)" />
        <KpiCard label="Critical" value={o ? fmtNumber(o.events.bySeverity.CRITICAL) : '--'} sub="Over-voltage" accent="var(--status-danger)" />
        <KpiCard label="Warning" value={o ? fmtNumber(o.events.bySeverity.WARNING) : '--'} sub="Under-voltage, overcurrent" accent="var(--status-warning)" />
        <KpiCard label="Notice" value={o ? fmtNumber(o.events.bySeverity.NOTICE) : '--'} sub="Load surges" accent="var(--accent-blue)" />
      </div>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h3 style={{ marginBottom: '1rem' }}>Search Incident Log</h3>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: 240, position: 'relative' }}>
            <Search size={18} style={{ position: 'absolute', left: '1rem', top: '0.8rem', color: 'var(--text-muted)' }} />
            <input
              className="input"
              style={{ paddingLeft: '2.75rem' }}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={onKey}
              placeholder="e.g. over_voltage, 2007-04, critical"
              aria-label="Search term"
            />
          </div>
          <select className="input" style={{ width: 'auto' }} value={algo} onChange={(e) => setAlgo(e.target.value)} aria-label="Search algorithm">
            {ALGORITHMS.map((a) => (
              <option key={a.value} value={a.value}>
                {a.label}
              </option>
            ))}
          </select>
          <button className="btn btn-primary" onClick={() => void runSearch()} disabled={searching}>
            {searching ? 'Searching...' : 'Search'}
          </button>
        </div>

        {searchError && <p style={{ color: 'var(--status-danger)', marginTop: '1rem' }}>{searchError}</p>}

        {result && (
          <div style={{ marginTop: '1.25rem' }}>
            <p className="muted" style={{ marginBottom: '0.75rem' }}>
              <strong style={{ color: 'var(--text-primary)' }}>{fmtNumber(result.matchedLines)}</strong> of {fmtNumber(result.logLines)} log entries matched · {fmtNumber(result.occurrences)} occurrences · {result.algorithm} scanned the log in{' '}
              {result.timeMicros < 1000 ? `${result.timeMicros} µs` : `${(result.timeMicros / 1000).toFixed(2)} ms`}
            </p>
            {result.results.length === 0 ? (
              <p className="muted">No entries match this search.</p>
            ) : (
              <div className="logbox">
                {result.results.map((e) => (
                  <div key={e.line} className="logline">
                    <span className="muted">#{e.line}</span> {highlight(logLine(e), result.query)}
                  </div>
                ))}
                {result.truncated && <div className="muted">Showing the first {result.results.length} matching entries.</div>}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '1rem' }}>
          <h3>Event History</h3>
          <div className="segmented">
            {TYPE_FILTERS.map((f) => (
              <button
                key={f.value}
                className={f.value === type ? 'active' : ''}
                onClick={() => {
                  setType(f.value);
                  setOffset(0);
                }}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        {events.error ? (
          <ErrorCard message={events.error} onRetry={events.reload} />
        ) : events.data ? (
          <>
            <table className="table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Event</th>
                  <th>Severity</th>
                  <th>Reading</th>
                  <th>Limit</th>
                </tr>
              </thead>
              <tbody>
                {events.data.events.map((e, i) => (
                  <tr key={`${e.timestamp}-${e.type}-${i}`}>
                    <td className="nowrap">{fmtDateTime(e.timestamp)}</td>
                    <td>{EVENT_LABELS[e.type] ?? e.type}</td>
                    <td>
                      <SeverityBadge severity={e.severity} />
                    </td>
                    <td>
                      {e.value} {e.unit}
                    </td>
                    <td className="muted">
                      {e.limit} {e.unit}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
              <span className="muted">
                {total === 0 ? 'No events' : `${offset + 1}–${Math.min(offset + PAGE_SIZE, total)} of ${fmtNumber(total)} (newest first)`}
              </span>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button className="btn btn-outline" disabled={offset === 0} onClick={() => setOffset(Math.max(0, offset - PAGE_SIZE))}>
                  Previous
                </button>
                <button className="btn btn-outline" disabled={offset + PAGE_SIZE >= total} onClick={() => setOffset(offset + PAGE_SIZE)}>
                  Next
                </button>
              </div>
            </div>
          </>
        ) : (
          <LoadingBlock height={300} />
        )}
      </div>
    </div>
  );
};

export default Faults;
