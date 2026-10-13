import { useEffect, useState } from 'react';
import { Timer, BarChart2 } from 'lucide-react';
import { API_BASE } from '../api';

const Benchmarks = () => {
  const [data, setData] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch(`${API_BASE}/api/benchmarks`)
      .then(res => res.json())
      .then(json => {
        if (json.status === 'error') {
          setError(json.message);
        } else {
          setData(json);
        }
      })
      .catch(() => {
        setError('Failed to fetch benchmarks. Backend may not be running or JMH hasn\'t finished.');
      });
  }, []);

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <Timer size={32} style={{ color: 'var(--accent-blue)' }} />
        <h1 style={{ margin: 0 }}>Performance Benchmarks</h1>
      </div>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Automated JMH benchmark results for the platform's search algorithms.
      </p>

      {error ? (
        <div className="card" style={{ borderLeft: '4px solid var(--status-warning)' }}>
          <p style={{ color: 'var(--status-warning)' }}>{error}</p>
        </div>
      ) : data ? (
        <div className="grid grid-cols-2">
          {data.map((bench: any, i: number) => (
            <div key={i} className="card">
              <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                <BarChart2 size={20} /> {bench.benchmark.split('.').pop()}
              </h3>
              <div style={{ padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '4px' }}>
                <p>Average Time: <strong style={{ color: 'var(--accent-cyan)' }}>{bench.primaryMetric.score.toFixed(3)}</strong> {bench.primaryMetric.scoreUnit}</p>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Margin of Error: ±{bench.primaryMetric.scoreError.toFixed(3)}</p>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Forks: {bench.forks}, Iterations: {bench.measurementIterations}</p>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p>Loading benchmark data...</p>
      )}
    </div>
  );
};

export default Benchmarks;
