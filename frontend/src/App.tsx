import type { CSSProperties, ReactNode } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { Activity, Cpu, Network, AlertTriangle, Zap, Server, Database, Timer, Layers } from 'lucide-react';

import Dashboard from './pages/Dashboard';
import Faults from './pages/Faults';
import Reports from './pages/Reports';
import Optimization from './pages/Optimization';
import NetworkFlow from './pages/NetworkFlow';
import NP from './pages/NP';
import ParallelLab from './pages/ParallelLab';
import Benchmarks from './pages/Benchmarks';
import GridAnalytics from './pages/GridAnalytics';
import { useApi } from './api';
import type { Health } from './types';
import { fmtDate, fmtNumber } from './format';
import { PAGE_IMAGES } from './pageImages';

const navGroups = [
  {
    title: 'Operations',
    items: [
      { path: '/', label: 'Overview Dashboard', icon: <Activity size={20} /> },
      { path: '/analytics', label: 'Grid Analytics', icon: <Zap size={20} /> },
      { path: '/faults', label: 'Incident Intelligence', icon: <AlertTriangle size={20} /> },
    ],
  },
  {
    title: 'Planning',
    items: [
      { path: '/network', label: 'Grid Capacity', icon: <Network size={20} /> },
      { path: '/optimization', label: 'Maintenance Optimization', icon: <Cpu size={20} /> },
      { path: '/np', label: 'Crew Scheduling', icon: <Server size={20} /> },
    ],
  },
  {
    title: 'Data & Reporting',
    items: [
      { path: '/reports', label: 'Maintenance Reports', icon: <Database size={20} /> },
      { path: '/parallel', label: 'Telemetry Processing', icon: <Layers size={20} /> },
      { path: '/benchmarks', label: 'Performance', icon: <Timer size={20} /> },
    ],
  },
];

const Sidebar = () => {
  const location = useLocation();

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <Zap className="sidebar-logo" size={28} />
        <h2>PowerGridX</h2>
      </div>
      <div className="sidebar-nav">
        {navGroups.map((group) => (
          <div key={group.title}>
            <div className="nav-section">{group.title}</div>
            {group.items.map((item) => (
              <Link key={item.path} to={item.path} className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}>
                {item.icon}
                <span>{item.label}</span>
              </Link>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
};

const Topbar = () => {
  const { data, error } = useApi<Health>('/api/health');
  const online = !!data && !error;

  return (
    <div className="topbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
        <span className="dot" style={{ background: online ? 'var(--status-success)' : error ? 'var(--status-danger)' : 'var(--text-muted)' }} />
        <h3 style={{ margin: 0, fontWeight: 500 }}>{online ? 'Data online' : error ? 'API unreachable' : 'Connecting...'}</h3>
      </div>
      {data && (
        <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          {fmtNumber(data.householdRows)} meter readings (to {fmtDate(data.householdTo)}) · {fmtNumber(data.zoneRows)} zone readings (to {fmtDate(data.zonesTo)})
        </span>
      )}
    </div>
  );
};

/** Page wrapper: shows the photo for the current page as a faded banner behind the page header. */
const PageFrame = ({ children }: { children: ReactNode }) => {
  const { pathname } = useLocation();
  const image = PAGE_IMAGES[pathname];
  const style = image ? ({ '--page-image': `url("${image}")` } as CSSProperties) : undefined;
  return (
    <div className="page-container" style={style}>
      {children}
    </div>
  );
};

function App() {
  return (
    <Router>
      <div className="app-container">
        <Sidebar />
        <div className="main-content">
          <Topbar />
          <PageFrame>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/analytics" element={<GridAnalytics />} />
              <Route path="/network" element={<NetworkFlow />} />
              <Route path="/faults" element={<Faults />} />
              <Route path="/optimization" element={<Optimization />} />
              <Route path="/reports" element={<Reports />} />
              <Route path="/np" element={<NP />} />
              <Route path="/parallel" element={<ParallelLab />} />
              <Route path="/benchmarks" element={<Benchmarks />} />
            </Routes>
          </PageFrame>
        </div>
      </div>
    </Router>
  );
}

export default App;
