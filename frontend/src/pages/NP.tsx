import { useState } from 'react';
import { BrainCircuit } from 'lucide-react';
import { apiGet, errorMessage } from '../api';
import { ErrorCard } from '../components/ui';

const NP = () => {
  const [data, setData] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  const calculateVC = async () => {
    try {
      setError(null);
      setData(await apiGet<any>('/api/np/vertex-cover'));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <BrainCircuit size={32} style={{ color: 'var(--accent-blue)' }} />
        <h1 style={{ margin: 0 }}>Maintenance Crew Scheduling</h1>
      </div>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Assign maintenance crews so that every pair of conflicting substations is covered, using either a fast approximate plan or an exact search.
      </p>

      {error && (
        <div style={{ marginBottom: '1.5rem' }}>
          <ErrorCard message={error} />
        </div>
      )}
      <div className="card" style={{ marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3>Crew Assignment</h3>
          <p style={{ color: 'var(--text-secondary)' }}>Find minimal maintenance teams needed to cover all conflicting substation zones.</p>
        </div>
        <button className="btn btn-primary" onClick={calculateVC}>Generate Crew Plan</button>
      </div>

      {data && (
        <div className="grid grid-cols-2">
          <div className="card">
            <h3>Fast Approximate Plan</h3>
            <div style={{ marginTop: '1rem', padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '4px' }}>
              <p>Substations assigned: <strong>{data.approxCover.join(', ')}</strong> (Size: {data.approxCover.length})</p>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Computed in {data.approxTimeNs / 1000} μs</p>
            </div>
            <p style={{ fontSize: '0.875rem', color: 'var(--status-success)', marginTop: '0.5rem' }}>
              Fast, and never more than twice the optimal crew size. Scales to large networks.
            </p>
          </div>

          <div className="card">
            <h3>Optimal Plan (Exact Search)</h3>
            <div style={{ marginTop: '1rem', padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '4px' }}>
              <p>Substations assigned: <strong>{data.exactCover.join(', ')}</strong> (Size: {data.exactCover.length})</p>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Computed in {data.exactTimeNs / 1000} μs</p>
            </div>
            <p style={{ fontSize: '0.875rem', color: 'var(--status-warning)', marginTop: '0.5rem' }}>
              Finds absolute minimum but scales poorly (O(2ⁿ)). Only feasible for small sub-grids.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default NP;
