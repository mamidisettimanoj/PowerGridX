import { useState } from 'react';
import { Database, FileText } from 'lucide-react';
import { apiGet, errorMessage } from '../api';
import { ErrorCard } from '../components/ui';

const Reports = () => {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchStructures = async () => {
    setLoading(true);
    try {
      setError(null);
      setData(await apiGet<any>('/api/reports/suffix'));
    } catch (err) {
      setError(errorMessage(err));
    }
    setLoading(false);
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <Database size={32} style={{ color: 'var(--accent-blue)' }} />
        <h1 style={{ margin: 0 }}>Maintenance Reports</h1>
      </div>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Generates the maintenance report from the loaded data and builds a searchable suffix index over it.
      </p>

      {error && (
        <div style={{ marginBottom: '1.5rem' }}>
          <ErrorCard message={error} />
        </div>
      )}
      <div className="card" style={{ marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3>{data ? `${data.title} (${data.period})` : 'Maintenance Report'}</h3>
          <p style={{ color: 'var(--text-secondary)' }}>Status: {data ? 'Indexed' : 'Not indexed'}</p>
        </div>
        <button className="btn btn-primary" onClick={fetchStructures} disabled={loading}>
          {loading ? 'Indexing...' : 'Generate & Index Report'}
        </button>
      </div>

      {data && (
        <div className="grid grid-cols-2">
          <div className="card">
            <h3 style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <FileText size={18} /> Raw Report Text
            </h3>
            <div style={{ 
              marginTop: '1rem', 
              padding: '1rem', 
              backgroundColor: 'var(--bg-secondary)', 
              borderRadius: 'var(--radius-sm)',
              fontFamily: 'var(--font-sans)',
              lineHeight: '1.6',
              whiteSpace: 'pre-wrap'
            }}>
              {data.text}
            </div>
            
            <div style={{ marginTop: '2rem' }}>
              <h3>Performance</h3>
              <p>Index built in <strong>{data.timeNs / 1000000} ms</strong></p>
              <p>Total suffixes: {data.sa.length}</p>
            </div>
          </div>

          <div className="card">
            <h3>Suffix Index</h3>
            <div style={{ 
              marginTop: '1rem',
              overflowY: 'auto',
              maxHeight: '400px',
              backgroundColor: 'var(--bg-secondary)',
              borderRadius: 'var(--radius-sm)',
              padding: '1rem'
            }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border-color)' }}>
                    <th style={{ padding: '0.5rem' }}>i</th>
                    <th style={{ padding: '0.5rem' }}>SA[i]</th>
                    <th style={{ padding: '0.5rem' }}>LCP[i]</th>
                    <th style={{ padding: '0.5rem' }}>Suffix Snippet</th>
                  </tr>
                </thead>
                <tbody>
                  {data.sa.map((saIndex: number, i: number) => (
                    <tr key={i} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                      <td style={{ padding: '0.5rem', color: 'var(--text-muted)' }}>{i}</td>
                      <td style={{ padding: '0.5rem', color: 'var(--accent-cyan)' }}>{saIndex}</td>
                      <td style={{ padding: '0.5rem', color: 'var(--status-warning)' }}>{data.lcp[i]}</td>
                      <td style={{ padding: '0.5rem', fontFamily: 'monospace' }}>
                        {data.text.substring(saIndex, Math.min(saIndex + 30, data.text.length)).replace(/\n/g, ' ')}...
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Reports;
