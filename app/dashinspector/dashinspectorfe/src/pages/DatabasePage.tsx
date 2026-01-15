import { Card } from '../components/Card/Card';
import './DatabasePage.css';

export default function DatabasePage() {
  return (
    <div className="database-page">
      <div className="page-header">
        <h2 className="page-title">Database</h2>
        <p className="page-subtitle">Browse SQLite, Room, and Realm databases</p>
      </div>

      <Card className="empty-state-card">
        <div className="empty-state-content">
          <div className="empty-icon-bg">
            <svg className="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <ellipse cx="12" cy="5" rx="9" ry="3" />
              <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" />
              <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" />
            </svg>
          </div>
          <h3>Coming Soon</h3>
          <p>The Database inspector is currently under development.</p>
        </div>
      </Card>
    </div>
  );
}
