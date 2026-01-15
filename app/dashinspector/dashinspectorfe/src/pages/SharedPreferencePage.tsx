import { useState, useEffect } from 'react';
import { preferencesApi } from '../services/api';
import type { PreferenceEntry, PreferenceFile } from '../types/preferences';
import Modal from '../components/Modal';
import './SharedPreferencePage.css';

type ModalType = 'createPref' | 'addEntry' | 'editEntry' | null;

const PREFERENCE_TYPES = ['String', 'Int', 'Long', 'Float', 'Boolean', 'StringSet'];

export default function SharedPreferencePage() {
  const [preferences, setPreferences] = useState<PreferenceFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedFiles, setExpandedFiles] = useState<Set<string>>(new Set());

  // Modal state
  const [modalType, setModalType] = useState<ModalType>(null);
  const [submitting, setSubmitting] = useState(false);

  // Form state
  const [formName, setFormName] = useState('');
  const [formKey, setFormKey] = useState('');
  const [formValue, setFormValue] = useState('');
  const [formType, setFormType] = useState('String');

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
      setExpandedFiles(prev => {
        // Keep existing expanded state, add new files as expanded
        const next = new Set(prev);
        data.forEach(f => {
          if (!prev.has(f.name) && prev.size === 0) {
            next.add(f.name);
          } else if (prev.size === 0) {
            next.add(f.name);
          }
        });
        // If it's the first load, expand all
        if (prev.size === 0) {
          return new Set(data.map(f => f.name));
        }
        return next;
      });
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
      case 'set':
      case 'stringset': return '#e91e63';
      default: return '#757575';
    }
  };

  const resetForm = () => {
    setFormName('');
    setFormKey('');
    setFormValue('');
    setFormType('String');
  };

  const closeModal = () => {
    setModalType(null);
    resetForm();
  };

  const openCreatePrefModal = () => {
    resetForm();
    setModalType('createPref');
  };

  const openAddEntryModal = (fileName: string, e: React.MouseEvent) => {
    e.stopPropagation();
    resetForm();
    setFormName(fileName);
    setModalType('addEntry');
  };

  const openEditEntryModal = (fileName: string, entry: PreferenceEntry, e: React.MouseEvent) => {
    e.stopPropagation();
    resetForm();
    setFormName(fileName);
    setFormKey(entry.key);
    setFormValue(entry.value);
    setFormType(entry.type);
    setModalType('editEntry');
  };

  const handleCreatePreference = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      await preferencesApi.createPreference({
        name: formName,
        key: formKey,
        value: formValue,
        type: formType,
      });
      closeModal();
      await loadPreferences();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to create preference');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAddEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      await preferencesApi.addEntry({
        name: formName,
        key: formKey,
        value: formValue,
        type: formType,
      });
      closeModal();
      await loadPreferences();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to add entry');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdateEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      await preferencesApi.updateEntry({
        name: formName,
        key: formKey,
        value: formValue,
        type: formType,
      });
      closeModal();
      await loadPreferences();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to update entry');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemovePreference = async (fileName: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm(`Are you sure you want to delete "${fileName}" and all its entries?`)) {
      return;
    }
    try {
      await preferencesApi.removePreference({ name: fileName });
      await loadPreferences();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to remove preference');
    }
  };

  const handleRemoveEntry = async (fileName: string, key: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm(`Are you sure you want to delete entry "${key}"?`)) {
      return;
    }
    try {
      await preferencesApi.removeEntry({ name: fileName, key });
      await loadPreferences();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to remove entry');
    }
  };

  const getModalTitle = () => {
    switch (modalType) {
      case 'createPref': return 'Create Preference';
      case 'addEntry': return 'Add Entry';
      case 'editEntry': return 'Edit Entry';
      default: return '';
    }
  };

  const getSubmitHandler = () => {
    switch (modalType) {
      case 'createPref': return handleCreatePreference;
      case 'addEntry': return handleAddEntry;
      case 'editEntry': return handleUpdateEntry;
      default: return () => {};
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
        <div className="page-header-content">
          <div>
            <h2>SharedPreference</h2>
            <p className="page-description">{preferences.length} preference file(s)</p>
          </div>
          <button className="btn-add-pref" onClick={openCreatePrefModal}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            Add Pref
          </button>
        </div>
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
                <span
                  className="btn-icon btn-add-entry"
                  onClick={(e) => openAddEntryModal(file.name, e)}
                  title="Add entry"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                </span>
                <span
                  className="btn-icon btn-remove-pref"
                  onClick={(e) => handleRemovePreference(file.name, e)}
                  title="Delete preference file"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="3 6 5 6 21 6" />
                    <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
                    <line x1="10" y1="11" x2="10" y2="17" />
                    <line x1="14" y1="11" x2="14" y2="17" />
                  </svg>
                </span>
              </button>

              {expandedFiles.has(file.name) && (
                <div className="entries-list">
                  {file.entries.length === 0 ? (
                    <div className="no-entries">No entries</div>
                  ) : (
                    file.entries.map((entry) => (
                      <div
                        key={entry.key}
                        className="entry-item"
                        onClick={(e) => openEditEntryModal(file.name, entry, e)}
                      >
                        <div className="entry-content">
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
                        <div className="entry-actions">
                          <span
                            className="btn-icon btn-edit-entry"
                            title="Edit entry"
                          >
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                              <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                            </svg>
                          </span>
                          <span
                            className="btn-icon btn-remove-entry"
                            onClick={(e) => handleRemoveEntry(file.name, entry.key, e)}
                            title="Delete entry"
                          >
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                              <polyline points="3 6 5 6 21 6" />
                              <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
                            </svg>
                          </span>
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

      {/* Modal for Create/Add/Edit */}
      <Modal isOpen={modalType !== null} onClose={closeModal} title={getModalTitle()}>
        <form onSubmit={getSubmitHandler()}>
          <div className="form-group">
            <label htmlFor="prefName">Preference Name</label>
            <input
              id="prefName"
              type="text"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              placeholder="e.g., user_settings"
              required
              disabled={modalType !== 'createPref'}
            />
          </div>

          <div className="form-group">
            <label htmlFor="entryKey">Key</label>
            <input
              id="entryKey"
              type="text"
              value={formKey}
              onChange={(e) => setFormKey(e.target.value)}
              placeholder="e.g., theme_mode"
              required
              disabled={modalType === 'editEntry'}
            />
          </div>

          <div className="form-group">
            <label htmlFor="entryType">Type</label>
            <select
              id="entryType"
              value={formType}
              onChange={(e) => setFormType(e.target.value)}
              disabled={modalType === 'editEntry'}
            >
              {PREFERENCE_TYPES.map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="entryValue">Value</label>
            <input
              id="entryValue"
              type="text"
              value={formValue}
              onChange={(e) => setFormValue(e.target.value)}
              placeholder="e.g., dark"
              required
            />
          </div>

          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={closeModal}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Saving...' : 'Save'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
