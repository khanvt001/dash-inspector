import { useState, useEffect } from 'react';
import { databaseApi } from '../services/api';
import type {
  DatabaseFile,
  TableInfo,
  ColumnInfo,
  TableDataResponse,
  ERDResponse,
} from '../types/database';
import Modal from '../components/Modal/Modal';
import { Button } from '../components/Button/Button';
import { Input } from '../components/Input/Input';
import { Card } from '../components/Card/Card';
import ERDViewer from '../components/ERDViewer/ERDViewer';
import './DatabasePage.css';

type ModalType = 'insert' | 'edit' | 'delete' | 'query' | null;

export default function DatabasePage() {
  // Database state
  const [databases, setDatabases] = useState<DatabaseFile[]>([]);
  const [selectedDb, setSelectedDb] = useState<string | null>(null);
  const [tables, setTables] = useState<TableInfo[]>([]);
  const [selectedTable, setSelectedTable] = useState<string | null>(null);
  const [isRoomDb, setIsRoomDb] = useState(false);

  // Data state
  const [tableData, setTableData] = useState<TableDataResponse | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(50);

  // ERD state
  const [erdData, setErdData] = useState<ERDResponse | null>(null);
  const [erdLoading, setErdLoading] = useState(false);

  // UI state
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedDbs, setExpandedDbs] = useState<Set<string>>(new Set());

  // Modal state
  const [modalType, setModalType] = useState<ModalType>(null);
  const [submitting, setSubmitting] = useState(false);
  const [editingRow, setEditingRow] = useState<Record<string, unknown> | null>(null);
  const [formValues, setFormValues] = useState<Record<string, string>>({});

  // View mode: 'data' or 'erd'
  const [viewMode, setViewMode] = useState<'data' | 'erd'>('data');

  useEffect(() => {
    loadDatabases();
  }, []);

  const loadDatabases = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await databaseApi.getAll();
      setDatabases(response.databases ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load databases');
    } finally {
      setLoading(false);
    }
  };

  const loadSchema = async (dbName: string) => {
    try {
      setError(null);
      const response = await databaseApi.getSchema({ database: dbName });
      setTables(response.tables ?? []);
      setIsRoomDb(response.isRoomDatabase);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load schema');
      setTables([]);
    }
  };

  const loadTableData = async (dbName: string, tableName: string, page: number = 1) => {
    try {
      setError(null);
      const response = await databaseApi.getTableData({
        database: dbName,
        table: tableName,
        page,
        pageSize,
      });
      setTableData(response);
      setCurrentPage(response.page);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load table data');
      setTableData(null);
    }
  };

  const loadERD = async (dbName: string) => {
    try {
      setErdLoading(true);
      setError(null);
      const response = await databaseApi.getERD({ database: dbName });
      setErdData(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load ERD');
      setErdData(null);
    } finally {
      setErdLoading(false);
    }
  };

  const toggleERDView = async () => {

    if (viewMode === 'erd') {
      setViewMode('data');
    } else {
      if (selectedDb && !erdData) {
        await loadERD(selectedDb);
      }
      setViewMode('erd');
    }
  };

  const handleDbClick = async (dbName: string) => {
    const isExpanded = expandedDbs.has(dbName);
    const next = new Set(expandedDbs);

    if (isExpanded) {
      next.delete(dbName);
    } else {
      next.add(dbName);
      if (selectedDb !== dbName) {
        setSelectedDb(dbName);
        setSelectedTable(null);
        setTableData(null);
        setErdData(null);
        setViewMode('data');
        await loadSchema(dbName);
      }
    }
    setExpandedDbs(next);
  };

  const handleTableClick = async (dbName: string, tableName: string) => {
    if (selectedDb !== dbName) {
      setSelectedDb(dbName);
      await loadSchema(dbName);
    }
    setSelectedTable(tableName);
    setViewMode('data');
    await loadTableData(dbName, tableName, 1);
  };

  const handlePageChange = (page: number) => {
    if (selectedDb && selectedTable) {
      loadTableData(selectedDb, selectedTable, page);
    }
  };

  const getSelectedTableInfo = (): TableInfo | undefined => {
    return tables.find(t => t.name === selectedTable);
  };

  const getPrimaryKeyColumns = (): ColumnInfo[] => {
    const tableInfo = getSelectedTableInfo();
    if (!tableInfo) return [];
    return tableInfo.columns.filter(c => c.pk > 0).sort((a, b) => a.pk - b.pk);
  };

  const getPrimaryKeyFromRow = (row: (string | number | boolean | null)[]): Record<string, unknown> => {
    const tableInfo = getSelectedTableInfo();
    if (!tableInfo || !tableData) return {};

    const pk: Record<string, unknown> = {};
    const pkColumns = getPrimaryKeyColumns();

    for (const col of pkColumns) {
      const colIndex = tableData.columns.indexOf(col.name);
      if (colIndex >= 0) {
        pk[col.name] = row[colIndex];
      }
    }
    return pk;
  };

  const openInsertModal = () => {
    const tableInfo = getSelectedTableInfo();
    if (!tableInfo) return;

    const initial: Record<string, string> = {};
    for (const col of tableInfo.columns) {
      initial[col.name] = col.defaultValue ?? '';
    }
    setFormValues(initial);
    setEditingRow(null);
    setModalType('insert');
  };

  const openEditModal = (row: (string | number | boolean | null)[]) => {
    if (!tableData) return;

    const values: Record<string, string> = {};
    for (let i = 0; i < tableData.columns.length; i++) {
      const val = row[i];
      values[tableData.columns[i]] = val !== null ? String(val) : '';
    }
    setFormValues(values);
    setEditingRow(getPrimaryKeyFromRow(row));
    setModalType('edit');
  };

  const openDeleteModal = (row: (string | number | boolean | null)[]) => {
    setEditingRow(getPrimaryKeyFromRow(row));
    setModalType('delete');
  };

  const closeModal = () => {
    setModalType(null);
    setEditingRow(null);
    setFormValues({});
  };

  const handleInsert = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDb || !selectedTable) return;

    try {
      setSubmitting(true);
      const values: Record<string, unknown> = {};
      for (const [key, val] of Object.entries(formValues)) {
        values[key] = val || null;
      }

      await databaseApi.insertRow({
        database: selectedDb,
        table: selectedTable,
        values,
      });

      closeModal();
      await loadTableData(selectedDb, selectedTable, currentPage);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to insert row');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDb || !selectedTable || !editingRow) return;

    try {
      setSubmitting(true);
      const values: Record<string, unknown> = {};
      const pkCols = getPrimaryKeyColumns().map(c => c.name);

      for (const [key, val] of Object.entries(formValues)) {
        if (!pkCols.includes(key)) {
          values[key] = val || null;
        }
      }

      await databaseApi.updateRow({
        database: selectedDb,
        table: selectedTable,
        primaryKey: editingRow,
        values,
      });

      closeModal();
      await loadTableData(selectedDb, selectedTable, currentPage);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to update row');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedDb || !selectedTable || !editingRow) return;

    try {
      setSubmitting(true);
      await databaseApi.deleteRow({
        database: selectedDb,
        table: selectedTable,
        primaryKey: editingRow,
      });

      closeModal();
      await loadTableData(selectedDb, selectedTable, currentPage);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to delete row');
    } finally {
      setSubmitting(false);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getTypeColor = (type: string): string => {
    const upper = type.toUpperCase();
    if (upper.includes('INT')) return 'var(--color-int)';
    if (upper.includes('TEXT') || upper.includes('CHAR') || upper.includes('CLOB')) return 'var(--color-string)';
    if (upper.includes('REAL') || upper.includes('FLOAT') || upper.includes('DOUBLE')) return 'var(--color-float)';
    if (upper.includes('BLOB')) return 'var(--color-set)';
    if (upper.includes('BOOL')) return 'var(--color-boolean)';
    return 'var(--text-tertiary)';
  };

  if (loading) {
    return (
      <div className="database-page">
        <div className="page-header">
          <h2 className="page-title">Database</h2>
          <p className="page-subtitle">Browse SQLite and Room databases</p>
        </div>
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Loading databases...</p>
        </div>
      </div>
    );
  }

  if (error && databases.length === 0) {
    return (
      <div className="database-page">
        <div className="page-header">
          <h2 className="page-title">Database</h2>
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
          <Button onClick={loadDatabases} variant="primary">Retry</Button>
        </div>
      </div>
    );
  }

  if (databases.length === 0) {
    return (
      <div className="database-page">
        <div className="page-header">
          <h2 className="page-title">Database</h2>
          <p className="page-subtitle">Browse SQLite and Room databases</p>
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
            <h3>No Databases Found</h3>
            <p>No SQLite or Room databases were found in this application.</p>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="database-page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Database</h2>
          <p className="page-subtitle">
            {databases.length} database{databases.length !== 1 ? 's' : ''} found
            {selectedDb && ` | ${selectedDb}`}
            {isRoomDb && ' (Room)'}
          </p>
        </div>
        <div className="header-actions">
          {selectedDb && (
            <Button
              variant={viewMode === 'erd' ? 'primary' : 'secondary'}
              onClick={toggleERDView}
              isLoading={erdLoading}
              leftIcon={
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="3" width="6" height="6" rx="1" />
                  <rect x="15" y="3" width="6" height="6" rx="1" />
                  <rect x="9" y="15" width="6" height="6" rx="1" />
                  <line x1="6" y1="9" x2="6" y2="12" />
                  <line x1="6" y1="12" x2="12" y2="12" />
                  <line x1="12" y1="12" x2="12" y2="15" />
                  <line x1="18" y1="9" x2="18" y2="12" />
                  <line x1="18" y1="12" x2="12" y2="12" />
                </svg>
              }
            >
              {viewMode === 'erd' ? 'Hide ERD' : 'View ERD'}
            </Button>
          )}
          {selectedTable && viewMode !== 'erd' && (
            <Button
              variant="primary"
              onClick={openInsertModal}
              leftIcon={
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
              }
            >
              Insert Row
            </Button>
          )}
        </div>
      </div>

      <div className="database-layout">
        {/* Sidebar - Database/Table List */}
        <Card className="database-sidebar" noPadding>
          <div className="sidebar-header">
            <span>Databases</span>
            <Button size="sm" variant="ghost" onClick={loadDatabases} title="Refresh">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
                <path d="M23 4v6h-6" />
                <path d="M1 20v-6h6" />
                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
              </svg>
            </Button>
          </div>
          <div className="sidebar-content">
            {databases.map((db) => (
              <div key={db.name} className="db-item">
                <div
                  className={`db-header ${selectedDb === db.name ? 'selected' : ''}`}
                  onClick={() => handleDbClick(db.name)}
                >
                  <div className="db-expand">
                    <svg
                      className={`expand-icon ${expandedDbs.has(db.name) ? 'expanded' : ''}`}
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                    >
                      <polyline points="9 18 15 12 9 6" />
                    </svg>
                  </div>
                  <div className="db-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                      <ellipse cx="12" cy="5" rx="9" ry="3" />
                      <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" />
                      <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" />
                    </svg>
                  </div>
                  <div className="db-info">
                    <span className="db-name">{db.name}</span>
                    <span className="db-size">{formatFileSize(db.size)}</span>
                  </div>
                  {db.isRoomDatabase && <span className="room-badge">Room</span>}
                </div>

                {expandedDbs.has(db.name) && selectedDb === db.name && (
                  <div className="table-list">
                    {tables.length === 0 ? (
                      <div className="no-tables">No tables found</div>
                    ) : (
                      tables.map((table) => (
                        <div
                          key={table.name}
                          className={`table-item ${selectedTable === table.name ? 'selected' : ''}`}
                          onClick={() => handleTableClick(db.name, table.name)}
                        >
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" width="14" height="14">
                            <rect x="3" y="3" width="18" height="18" rx="2" />
                            <line x1="3" y1="9" x2="21" y2="9" />
                            <line x1="9" y1="21" x2="9" y2="9" />
                          </svg>
                          <span className="table-name">{table.name}</span>
                          <span className="table-count">{table.rowCount}</span>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        </Card>

        {/* Main Content Area */}
        <div className="database-content">
          {/* ERD View */}
          {viewMode === 'erd' && selectedDb ? (
            erdData ? (
              <div className="erd-container">
                <ERDViewer
                  tables={erdData.tables}
                  relationships={erdData.relationships}
                />
              </div>
            ) : erdLoading ? (
              <Card className="empty-state-card">
                <div className="loading-state">
                  <div className="spinner"></div>
                  <p>Loading ERD...</p>
                </div>
              </Card>
            ) : (
              <Card className="empty-state-card">
                <div className="empty-state-content">
                  <p>Failed to load ERD data</p>
                  <Button variant="primary" onClick={() => loadERD(selectedDb)}>Retry</Button>
                </div>
              </Card>
            )
          ) : selectedTable && tableData ? (
            <Card className="data-card" noPadding>
              <div className="data-header">
                <div className="data-header-left">
                  <span>{selectedTable}</span>
                  <span className="data-info">
                    {tableData.totalRows} row{tableData.totalRows !== 1 ? 's' : ''}
                  </span>
                </div>
                <div className="schema-toggle">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => {
                      const tableInfo = getSelectedTableInfo();
                      if (tableInfo) {
                        alert(
                          `Schema for ${selectedTable}:\n\n` +
                          tableInfo.columns.map(c =>
                            `${c.name} (${c.type})${c.pk > 0 ? ' [PK]' : ''}${c.notnull ? ' NOT NULL' : ''}`
                          ).join('\n')
                        );
                      }
                    }}
                    title="View Schema"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
                      <circle cx="12" cy="12" r="10" />
                      <line x1="12" y1="16" x2="12" y2="12" />
                      <line x1="12" y1="8" x2="12.01" y2="8" />
                    </svg>
                  </Button>
                </div>
              </div>
              <div className="data-table-wrapper">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th className="action-column"></th>
                      {tableData.columns.map((col) => {
                        const colInfo = getSelectedTableInfo()?.columns.find(c => c.name === col);
                        return (
                          <th key={col}>
                            <div className="column-header">
                              <span>{col}</span>
                              {colInfo && (
                                <span
                                  className="column-type"
                                  style={{ color: getTypeColor(colInfo.type) }}
                                >
                                  {colInfo.type}
                                </span>
                              )}
                              {colInfo?.pk ? (
                                <svg viewBox="0 0 24 24" fill="currentColor" width="12" height="12" className="pk-icon">
                                  <path d="M12 2C9.24 2 7 4.24 7 7c0 1.8.96 3.37 2.4 4.24L7 22h10l-2.4-10.76C16.04 10.37 17 8.8 17 7c0-2.76-2.24-5-5-5zm0 7c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z" />
                                </svg>
                              ) : null}
                            </div>
                          </th>
                        );
                      })}
                    </tr>
                  </thead>
                  <tbody>
                    {tableData.rows.map((row, idx) => (
                      <tr key={idx}>
                        <td className="action-column">
                          <div className="row-actions">
                            <button className="row-action-btn" onClick={() => openEditModal(row)} title="Edit">
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="14" height="14">
                                <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                                <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                              </svg>
                            </button>
                            <button className="row-action-btn delete" onClick={() => openDeleteModal(row)} title="Delete">
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="14" height="14">
                                <polyline points="3 6 5 6 21 6" />
                                <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
                              </svg>
                            </button>
                          </div>
                        </td>
                        {row.map((cell, cellIdx) => (
                          <td key={cellIdx} className={cell === null ? 'null-value' : ''}>
                            {cell === null ? 'NULL' : String(cell)}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {tableData.totalPages > 1 && (
                <div className="pagination">
                  <Button
                    size="sm"
                    variant="ghost"
                    disabled={currentPage <= 1}
                    onClick={() => handlePageChange(currentPage - 1)}
                  >
                    Previous
                  </Button>
                  <span className="page-info">
                    Page {currentPage} of {tableData.totalPages}
                  </span>
                  <Button
                    size="sm"
                    variant="ghost"
                    disabled={currentPage >= tableData.totalPages}
                    onClick={() => handlePageChange(currentPage + 1)}
                  >
                    Next
                  </Button>
                </div>
              )}
            </Card>
          ) : selectedDb && !selectedTable ? (
            <Card className="empty-state-card">
              <div className="empty-state-content">
                <div className="empty-icon-bg">
                  <svg className="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <rect x="3" y="3" width="18" height="18" rx="2" />
                    <line x1="3" y1="9" x2="21" y2="9" />
                    <line x1="9" y1="21" x2="9" y2="9" />
                  </svg>
                </div>
                <h3>Select a Table</h3>
                <p>Choose a table from the sidebar to view its data.</p>
              </div>
            </Card>
          ) : (
            <Card className="empty-state-card">
              <div className="empty-state-content">
                <div className="empty-icon-bg">
                  <svg className="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <ellipse cx="12" cy="5" rx="9" ry="3" />
                    <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" />
                    <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" />
                  </svg>
                </div>
                <h3>Select a Database</h3>
                <p>Choose a database from the sidebar to browse its tables.</p>
              </div>
            </Card>
          )}
        </div>
      </div>

      {/* Insert/Edit Modal */}
      <Modal
        isOpen={modalType === 'insert' || modalType === 'edit'}
        onClose={closeModal}
        title={modalType === 'insert' ? 'Insert Row' : 'Edit Row'}
      >
        <form onSubmit={modalType === 'insert' ? handleInsert : handleUpdate} className="modal-form">
          {getSelectedTableInfo()?.columns.map((col) => {
            const isPk = col.pk > 0;
            return (
              <Input
                key={col.name}
                label={
                  <span>
                    {col.name}
                    <span style={{ color: getTypeColor(col.type), marginLeft: '8px', fontSize: '12px' }}>
                      {col.type}
                    </span>
                    {isPk && <span style={{ color: 'var(--warning-color)', marginLeft: '4px' }}>(PK)</span>}
                    {col.notnull && <span style={{ color: 'var(--danger-color)', marginLeft: '4px' }}>*</span>}
                  </span>
                }
                value={formValues[col.name] ?? ''}
                onChange={(e) => setFormValues(prev => ({ ...prev, [col.name]: e.target.value }))}
                placeholder={col.defaultValue ?? `Enter ${col.type}`}
                disabled={modalType === 'edit' && isPk}
                required={col.notnull && !isPk}
                fullWidth
              />
            );
          })}
          <div className="modal-actions">
            <Button type="button" variant="ghost" onClick={closeModal}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={submitting}>
              {modalType === 'insert' ? 'Insert' : 'Save'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={modalType === 'delete'}
        onClose={closeModal}
        title="Delete Row"
      >
        <div className="delete-modal-content">
          <p>Are you sure you want to delete this row?</p>
          {editingRow && (
            <div className="delete-pk-info">
              <strong>Primary Key:</strong>
              <code>{JSON.stringify(editingRow)}</code>
            </div>
          )}
          <div className="modal-actions">
            <Button type="button" variant="ghost" onClick={closeModal}>
              Cancel
            </Button>
            <Button
              type="button"
              variant="danger"
              onClick={handleDelete}
              isLoading={submitting}
            >
              Delete
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
