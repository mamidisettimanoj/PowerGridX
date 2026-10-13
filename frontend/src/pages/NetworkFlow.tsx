import { useState } from 'react';
import { Network, Activity } from 'lucide-react';
import { apiGet, errorMessage } from '../api';
import { ErrorCard } from '../components/ui';

const NetworkFlow = () => {
  const [algo, setAlgo] = useState('edmonds-karp');
  const [res, setRes] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  const calculateFlow = async () => {
    try {
      setError(null);
      setRes(await apiGet<any>(`/api/network/flow?algo=${algo}`));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <Network size={32} style={{ color: 'var(--accent-cyan)' }} />
        <h1 style={{ margin: 0 }}>Grid Capacity Analysis</h1>
      </div>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Computes the maximum power that can be delivered from the generator to the city across the transmission network (sample six-substation network).
      </p>

      {error && (
        <div style={{ marginBottom: '1.5rem' }}>
          <ErrorCard message={error} />
        </div>
      )}
      <div className="card" style={{ marginBottom: '2rem', display: 'flex', gap: '1rem', alignItems: 'center' }}>
        <select 
          value={algo} 
          onChange={(e) => setAlgo(e.target.value)}
          style={{
            padding: '0.75rem 1rem',
            backgroundColor: 'var(--bg-secondary)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            color: 'var(--text-primary)'
          }}
        >
          <option value="edmonds-karp">Edmonds-Karp</option>
          <option value="dinic">Dinic</option>
        </select>
        <button className="btn btn-primary" onClick={calculateFlow}>Compute Capacity</button>
      </div>

      <div className="grid grid-cols-2">
        <div className="card">
          <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
            <Activity size={20} /> Capacity Results
          </h3>
          {res ? (
            <div>
              <p style={{ fontSize: '2rem', fontWeight: 'bold', color: 'var(--status-success)', marginBottom: '0.5rem' }}>
                {res.maxFlow} MW
              </p>
              <p style={{ color: 'var(--text-secondary)' }}>Maximum theoretical transfer capacity from Generator to City.</p>
              
              <div style={{ marginTop: '1rem', padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '4px' }}>
                <p><strong>Algorithm:</strong> {res.algorithm}</p>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Computed in {res.timeNs / 1000} μs</p>
              </div>
            </div>
          ) : (
            <p style={{ color: 'var(--text-muted)' }}>Run computation to see maximum flow capacity.</p>
          )}
        </div>
        
        <div className="card">
          <h3>Line Capacity Matrix</h3>
          {res ? (
            <div style={{ 
              marginTop: '1rem', 
              backgroundColor: 'var(--bg-secondary)', 
              padding: '1rem', 
              borderRadius: 'var(--radius-sm)',
              fontFamily: 'monospace',
              fontSize: '0.875rem',
              overflowX: 'auto'
            }}>
              {res.capacities.map((row: number[], i: number) => (
                <div key={i} style={{ display: 'flex', gap: '1rem', marginBottom: '0.5rem' }}>
                  {row.map((val, j) => (
                    <span key={j} style={{ width: '20px', textAlign: 'right', color: val > 0 ? 'var(--accent-cyan)' : 'var(--text-muted)' }}>
                      {val}
                    </span>
                  ))}
                </div>
              ))}
            </div>
          ) : (
            <p style={{ marginTop: '1rem', color: 'var(--text-muted)' }}>Matrix will appear after computation.</p>
          )}
        </div>
      </div>
    </div>
  );
};

export default NetworkFlow;
