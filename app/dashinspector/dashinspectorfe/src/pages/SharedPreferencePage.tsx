import { useState, useEffect } from 'react';
import { preferencesApi } from '../services/api';
import type { PreferenceEntry, PreferenceFile } from '../types/preferences';
import Modal from '../components/Modal/Modal';
import { Button } from '../components/Button/Button';
import { Input } from '../components/Input/Input';
import { Card } from '../components/Card/Card';
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
      case 'string': return 'var(--color-string)';
      case 'int':
      case 'integer': return 'var(--color-int)';
      case 'long': return 'var(--color-long)';
      case 'float': return 'var(--color-float)';
      case 'boolean': return 'var(--color-boolean)';
      case 'set':
      case 'stringset': return 'var(--color-set)';
      default: return 'var(--text-tertiary)';
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
      default: return () => { };
    }
  };

  if (loading) {
    return (
      <div className="shared-preference-page">
        <div className="page-header">
          <div>
            <h2 className="page-title">SharedPreference</h2>
            <p className="page-subtitle">Manage your application's shared preferences</p>
          </div>
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
          <h2 className="page-title">SharedPreference</h2>
        </div>
        <div className="error-state">
          <div className="error-icon-bg">
            <svg className="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          </div>
          <h3>Failed to load data</h3>
          <p>{error}</p>
          <Button onClick={loadPreferences} variant="primary">Retry</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="shared-preference-page">
      <div className="page-header">
        <div>
          <h2 className="page-title">SharedPreference</h2>
          <p className="page-subtitle">{preferences.length} preference file{preferences.length !== 1 ? 's' : ''} found</p>
        </div>
        <Button
          variant="primary"
          onClick={openCreatePrefModal}
          leftIcon={
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          }
        >
          New File
        </Button>
      </div>

      <div className="preferences-container">
        {preferences.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon-bg">
              <svg className="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M12 2L2 7l10 5 10-5-10-5z" />
                <path d="M2 17l10 5 10-5" />
                <path d="M2 12l10 5 10-5" />
              </svg>
            </div>
            <h3>No Preferences Found</h3>
            <p>Create a new preference file to get started.</p>
            <Button onClick={openCreatePrefModal} variant="secondary" className="mt-4">
              Create Preference
            </Button>
          </div>
        ) : (
          <div className="preferences-list">
            {preferences.map((file) => (
              <Card key={file.name} noPadding className="preference-card">
                <div
                  className={`file-header ${expandedFiles.has(file.name) ? 'expanded' : ''}`}
                  onClick={() => toggleFile(file.name)}
                >
                  <div className="file-info">
                    <div className="expand-icon-wrapper">
                      <svg className="expand-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="9 18 15 12 9 6" />
                      </svg>
                    </div>
                    <span className="file-name">{file.name}</span>
                    <span className="badge">{file.entries.length} entries</span>
                  </div>

                  <div className="file-actions">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={(e) => openAddEntryModal(file.name, e)}
                      title="Add entry"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <line x1="5" y1="12" x2="19" y2="12" />
                      </svg>
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="text-danger"
                      onClick={(e) => handleRemovePreference(file.name, e)}
                      title="Delete preference file"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
                        <polyline points="3 6 5 6 21 6" />
                        <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
                      </svg>
                    </Button>
                  </div>
                </div>

                {expandedFiles.has(file.name) && (
                  <div className="entries-list">
                    {file.entries.length === 0 ? (
                      <div className="no-entries">No entries in this file</div>
                    ) : (
                      file.entries.map((entry) => (
                        <div
                          key={entry.key}
                          className="entry-item"
                          onClick={(e) => openEditEntryModal(file.name, entry, e)}
                        >
                          <div className="entry-content">
                            <div className="entry-main-row">
                              <span className="entry-key" title={entry.key}>{entry.key}</span>
                              <div className="entry-type-wrapper">
                                <span
                                  className="entry-type-badge"
                                  style={{
                                    color: getTypeColor(entry.type),
                                    borderColor: getTypeColor(entry.type),
                                    backgroundColor: `color-mix(in srgb, ${getTypeColor(entry.type)}, transparent 90%)`
                                  }}
                                >
                                  {entry.type}
                                </span>
                              </div>
                            </div>
                            <div className="entry-value-row">
                              <span className="entry-value">{entry.value}</span>
                            </div>
                          </div>
                          <div className="entry-actions">
                            <Button
                              size="sm"
                              variant="ghost"
                              className="text-danger"
                              onClick={(e) => handleRemoveEntry(file.name, entry.key, e)}
                              title="Delete entry"
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
                                <polyline points="3 6 5 6 21 6" />
                                <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
                              </svg>
                            </Button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </Card>
            ))}
          </div>
        )}
      </div>

      <Modal isOpen={modalType !== null} onClose={closeModal} title={getModalTitle()}>
        <form onSubmit={getSubmitHandler()} className="modal-form">
          <Input
            label="Preference Name"
            value={formName}
            onChange={(e) => setFormName(e.target.value)}
            placeholder="e.g., user_settings"
            required
            disabled={modalType !== 'createPref'}
            fullWidth
          />

          <Input
            label="Key"
            value={formKey}
            onChange={(e) => setFormKey(e.target.value)}
            placeholder="e.g., theme_mode"
            required
            disabled={modalType === 'editEntry'}
            fullWidth
          />

          <div className="form-group">
            <label htmlFor="entryType" className="input-label">Type</label>
            <select
              id="entryType"
              className="select-input"
              value={formType}
              onChange={(e) => setFormType(e.target.value)}
              disabled={modalType === 'editEntry'}
            >
              {PREFERENCE_TYPES.map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </div>

          <Input
            label="Value"
            value={formValue}
            onChange={(e) => setFormValue(e.target.value)}
            placeholder="e.g., dark"
            required
            fullWidth
          />

          <div className="modal-actions">
            <Button type="button" variant="ghost" onClick={closeModal}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={submitting}>
              Save
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
