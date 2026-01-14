import './DatabasePage.css';

export default function DatabasePage() {
  return (
    <div className="database-page">
      <div className="page-header">
        <h2>Database</h2>
        <p className="page-description">Browse SQLite, Room, and Realm databases</p>
      </div>
      <div className="content-placeholder">
        <svg className="placeholder-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <ellipse cx="12" cy="5" rx="9" ry="3" />
          <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" />
          <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" />
        </svg>
        <p>Database inspector will be implemented here</p>
      </div>
    </div>
  );
}
