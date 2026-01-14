import { useState, useEffect } from 'react';
import { preferencesApi } from '../services/api';
import type { PreferenceFile } from '../types/preferences';
import './SharedPreferencePage.css';

export default function SharedPreferencePage() {
  const [preferences, setPreferences] = useState<PreferenceFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedFiles, setExpandedFiles] = useState<Set<string>>(new Set());

  useEffect(() => {
    loadPreferences();
  }, []);

  const loadPreferences = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await preferencesApi.getAll();
      const data = response.preferences ?? [];
      setPreferences(data);
      // Expand all files by default
      setExpandedFiles(new Set(data.map(f => f.name)));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load preferences');
    } finally {
      setLoading(false);
    }
  };

  const toggleFile = (fileName: string) => {
    setExpandedFiles(prev => {
      const next = new Set(prev);
      if (next.has(fileName)) {
        next.delete(fileName);
      } else {
        next.add(fileName);
      }
      return next;
    });
  };

  const getTypeColor = (type: string): string => {
    switch (type.toLowerCase()) {
      case 'string': return '#4caf50';
      case 'int':
      case 'integer': return '#2196f3';
      case 'long': return '#03a9f4';
      case 'float': return '#ff9800';
      case 'boolean': return '#9c27b0';
      case 'set': return '#e91e63';
      default: return '#757575';
    }
  };

  if (loading) {
    return (
      <div className="shared-preference-page">
        <div className="page-header">
          <h2>SharedPreference</h2>
        </div>
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Loading preferences...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="shared-preference-page">
        <div className="page-header">
          <h2>SharedPreference</h2>
        </div>
        <div className="error-state">
          <svg className="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <p>{error}</p>
          <button onClick={loadPreferences}>Retry</button>
        </div>
      </div>
    );
  }

  return (
    <div className="shared-preference-page">
      <div className="page-header">
        <h2>SharedPreference</h2>
        <p className="page-description">{preferences.length} preference file(s)</p>
      </div>

      {preferences.length === 0 ? (
        <div className="empty-state">
          <svg className="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M12 2L2 7l10 5 10-5-10-5z" />
            <path d="M2 17l10 5 10-5" />
            <path d="M2 12l10 5 10-5" />
          </svg>
          <p>No SharedPreference files found</p>
        </div>
      ) : (
        <div className="preferences-list">
          {preferences.map((file) => (
            <div key={file.name} className="preference-file">
              <button
                className={`file-header ${expandedFiles.has(file.name) ? 'expanded' : ''}`}
                onClick={() => toggleFile(file.name)}
              >
                <svg className="expand-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="9 18 15 12 9 6" />
                </svg>
                <span className="file-name">{file.name}</span>
                <span className="entry-count">{file.entries.length} entries</span>
              </button>

              {expandedFiles.has(file.name) && (
                <div className="entries-list">
                  {file.entries.length === 0 ? (
                    <div className="no-entries">No entries</div>
                  ) : (
                    file.entries.map((entry) => (
                      <div key={entry.key} className="entry-item">
                        <div className="entry-key">{entry.key}</div>
                        <div className="entry-meta">
                          <span
                            className="entry-type"
                            style={{ backgroundColor: getTypeColor(entry.type) }}
                          >
                            {entry.type}
                          </span>
                          <span className="entry-value">{entry.value}</span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
