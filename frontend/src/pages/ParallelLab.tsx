import { useState } from 'react';
import { Layers, Activity, FastForward } from 'lucide-react';
import { apiGet, errorMessage } from '../api';
import { ErrorCard } from '../components/ui';

const ParallelLab = () => {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const runAnalysis = async () => {
    setLoading(true);
    try {
      setError(null);
      setData(await apiGet<any>('/api/parallel/process'));
    } catch (e) {
      setError(errorMessage(e));
    }
    setLoading(false);
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <Layers size={32} style={{ color: 'var(--accent-cyan)' }} />
        <h1 style={{ margin: 0 }}>Telemetry Processing</h1>
      </div>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Processes every household meter reading using parallel aggregation and uniform random sampling.
      </p>

      {error && (
        <div style={{ marginBottom: '1.5rem' }}>
          <ErrorCard message={error} />
        </div>
      )}
      <div className="card" style={{ marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3>Meter Telemetry</h3>
          <p style={{ color: 'var(--text-secondary)' }}>Aggregates every loaded household meter reading in one run.</p>
        </div>
        <button className="btn btn-primary" onClick={runAnalysis} disabled={loading}>
          {loading ? 'Processing...' : 'Run Parallel Analytics'}
        </button>
      </div>

      {data && (
        <div className="grid grid-cols-2">
          <div className="card">
            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Activity size={20} /> Reservoir Sampling
            </h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>
              Uniform random sample of 10 readings from {data.dataSize.toLocaleString()} records in a single pass.
            </p>
            <div style={{ padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '4px' }}>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                {data.sample.map((val: number, i: number) => (
                  <span key={i} style={{ padding: '0.25rem 0.5rem', background: 'var(--bg-panel)', borderRadius: '4px', fontSize: '0.875rem' }}>
                    {val.toFixed(3)}
                  </span>
                ))}
              </div>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '1rem' }}>
                Sampled in {(data.sampleTimeNs / 1000).toFixed(1)} μs
              </p>
            </div>
          </div>

          <div className="card">
            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <FastForward size={20} /> Parallel vs Sequential Reduce
            </h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>
              Total active power computed sequentially and in parallel (average of {data.timedRuns} runs on {data.cores} cores).
            </p>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '4px', borderLeft: '4px solid var(--status-warning)' }}>
                <h4>Sequential Result</h4>
                <p style={{ fontSize: '1.25rem', margin: '0.5rem 0' }}>{data.seqSum.toFixed(2)} kW</p>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Time: {(data.seqTimeNs / 1000000).toFixed(3)} ms</p>
              </div>

              <div style={{ padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '4px', borderLeft: '4px solid var(--status-success)' }}>
                <h4>Parallel Result</h4>
                <p style={{ fontSize: '1.25rem', margin: '0.5rem 0' }}>{data.parSum.toFixed(2)} kW</p>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Time: {(data.parTimeNs / 1000000).toFixed(3)} ms</p>
              </div>
              
              <div style={{ fontSize: '0.875rem', color: data.parTimeNs < data.seqTimeNs ? 'var(--status-success)' : 'var(--status-warning)' }}>
                Speedup: {(data.seqTimeNs / data.parTimeNs).toFixed(2)}x
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ParallelLab;
