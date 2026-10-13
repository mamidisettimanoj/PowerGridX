import { useState } from 'react';
import { Cpu, Navigation, Type } from 'lucide-react';
import { apiGet, errorMessage } from '../api';
import { ErrorCard } from '../components/ui';

const Optimization = () => {
  const [fuzzyQuery, setFuzzyQuery] = useState('TRNSFORMER');
  const [fuzzyTarget, setFuzzyTarget] = useState('TRANSFORMER');
  const [fuzzyRes, setFuzzyRes] = useState<any>(null);

  const [droneRes, setDroneRes] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  const runFuzzy = async () => {
    try {
      setError(null);
      setFuzzyRes(await apiGet<any>(`/api/optimization/fuzzy?query=${encodeURIComponent(fuzzyQuery)}&target=${encodeURIComponent(fuzzyTarget)}`));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const runDrone = async () => {
    try {
      setError(null);
      setDroneRes(await apiGet<any>('/api/optimization/drone'));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <Cpu size={32} style={{ color: 'var(--status-warning)' }} />
        <h1 style={{ margin: 0 }}>Maintenance Optimization</h1>
      </div>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Equipment lookup that tolerates typos, and the shortest inspection route for maintenance drones.
      </p>

      {error && (
        <div style={{ marginBottom: '1.5rem' }}>
          <ErrorCard message={error} />
        </div>
      )}
      <div className="grid grid-cols-2">
        <div className="card">
          <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Type size={20} /> Fuzzy Equipment Search
          </h3>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>
            Measures how many edits separate a typed name from the equipment record, so misspelled queries still match.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '1rem' }}>
            <input 
              value={fuzzyQuery} onChange={e => setFuzzyQuery(e.target.value)} 
              placeholder="Query (e.g. TRNSFORMER)"
              style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-secondary)', color: 'white' }}
            />
            <input 
              value={fuzzyTarget} onChange={e => setFuzzyTarget(e.target.value)} 
              placeholder="Target (e.g. TRANSFORMER)"
              style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-secondary)', color: 'white' }}
            />
          </div>
          <button className="btn btn-primary" onClick={runFuzzy}>Calculate Edit Distance</button>
          
          {fuzzyRes && (
            <div style={{ marginTop: '1rem', padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '4px' }}>
              <p>Distance: <strong style={{ color: 'var(--accent-cyan)' }}>{fuzzyRes.distance}</strong> edits</p>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Computed in {fuzzyRes.timeNs / 1000} μs</p>
            </div>
          )}
        </div>

        <div className="card">
          <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Navigation size={20} /> Drone Inspection Route
          </h3>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>
            Finds the cheapest route for a maintenance drone that must visit every substation once and return to base.
          </p>
          
          <button className="btn btn-primary" onClick={runDrone}>Optimize Route</button>
          
          {droneRes && (
            <div style={{ marginTop: '1rem', padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '4px' }}>
              <p>Minimum Travel Cost: <strong style={{ color: 'var(--status-success)' }}>{droneRes.minCost}</strong></p>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Computed in {droneRes.timeNs / 1000} μs</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Optimization;
