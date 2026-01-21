// Database file information
export interface DatabaseFile {
  name: string;
  path: string;
  size: number;
  isRoomDatabase: boolean;
}

// Column information from PRAGMA table_info
export interface ColumnInfo {
  cid: number;
  name: string;
  type: string;
  notnull: boolean;
  defaultValue: string | null;
  pk: number; // Primary key indicator (0 = not pk, 1+ = pk order)
}

// Table information with columns and row count
export interface TableInfo {
  name: string;
  columns: ColumnInfo[];
  rowCount: number;
}

// Response for listing all databases
export interface DatabaseListResponse {
  databases: DatabaseFile[];
  total: number;
}

// Response for database schema
export interface DatabaseSchemaResponse {
  database: string;
  tables: TableInfo[];
  isRoomDatabase: boolean;
}

// Response for paginated table data
export interface TableDataResponse {
  database: string;
  table: string;
  columns: string[];
  rows: (string | number | boolean | null)[][];
  totalRows: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

// Response for SQL query results
export interface QueryResponse {
  database: string;
  query: string;
  columns: string[];
  rows: (string | number | boolean | null)[][];
  rowCount: number;
  executionTimeMs: number;
}

// Request types
export interface DatabaseRequest {
  database: string;
}

export interface TableDataRequest {
  database: string;
  table: string;
  page?: number;
  pageSize?: number;
}

export interface QueryRequest {
  database: string;
  query: string;
}

export interface UpdateRowRequest {
  database: string;
  table: string;
  primaryKey: Record<string, unknown>;
  values: Record<string, unknown>;
}

export interface DeleteRowRequest {
  database: string;
  table: string;
  primaryKey: Record<string, unknown>;
}

export interface InsertRowRequest {
  database: string;
  table: string;
  values: Record<string, unknown>;
}

// ERD Types
export interface ForeignKeyInfo {
  id: number;
  seq: number;
  table: string;
  from: string;
  to: string;
  onUpdate: string;
  onDelete: string;
}

export interface ERDTableInfo {
  name: string;
  columns: ColumnInfo[];
  foreignKeys: ForeignKeyInfo[];
}

export interface ERDRelationship {
  fromTable: string;
  fromColumn: string;
  toTable: string;
  toColumn: string;
  relationshipType: 'one-to-one' | 'one-to-many' | 'many-to-one';
}

export interface ERDResponse {
  database: string;
  tables: ERDTableInfo[];
  relationships: ERDRelationship[];
  isRoomDatabase: boolean;
}
