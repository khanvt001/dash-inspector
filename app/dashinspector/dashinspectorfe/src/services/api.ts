const API_BASE_URL = 'http://localhost:8080/api';

export async function fetchJson<T>(endpoint: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`);
  if (!response.ok) {
    // Try to extract error message from response body
    try {
      const errorData = await response.json();
      const errorMessage = errorData.error || errorData.message || `HTTP error! status: ${response.status}`;
      throw new Error(errorMessage);
    } catch (parseError) {
      // If we can't parse the response, fall back to status code
      throw new Error(`HTTP error! status: ${response.status}`);
    }
  }
  return response.json();
}

export async function postJson<T>(endpoint: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    // Try to extract error message from response body
    try {
      const errorData = await response.json();
      const errorMessage = errorData.error || errorData.message || `HTTP error! status: ${response.status}`;
      throw new Error(errorMessage);
    } catch (parseError) {
      // If we can't parse the response, fall back to status code
      throw new Error(`HTTP error! status: ${response.status} - ${parseError}`);
    }
  }
  return response.json();
}

export interface PreferenceEntryRequest {
  name: string;
  key: string;
  value: string;
  type: string;
}

export interface RemoveEntryRequest {
  name: string;
  key: string;
}

export interface RemovePreferenceRequest {
  name: string;
}

export const preferencesApi = {
  getAll: () => fetchJson<import('../types/preferences').PreferencesResponse>('/preferences'),
  createPreference: (data: PreferenceEntryRequest) => postJson('/preferences/create', data),
  addEntry: (data: PreferenceEntryRequest) => postJson('/preferences/entry/add', data),
  updateEntry: (data: PreferenceEntryRequest) => postJson('/preferences/entry/update', data),
  removeEntry: (data: RemoveEntryRequest) => postJson('/preferences/entry/remove', data),
  removePreference: (data: RemovePreferenceRequest) => postJson('/preferences/remove', data),
};

export const databaseApi = {
  getAll: () => fetchJson<import('../types/database').DatabaseListResponse>('/database'),
  getSchema: (data: import('../types/database').DatabaseRequest) =>
    postJson<import('../types/database').DatabaseSchemaResponse>('/database/schema', data),
  getERD: (data: import('../types/database').DatabaseRequest) =>
    postJson<import('../types/database').ERDResponse>('/database/erd', data),
  getTableData: (data: import('../types/database').TableDataRequest) =>
    postJson<import('../types/database').TableDataResponse>('/database/table/data', data),
  executeQuery: (data: import('../types/database').QueryRequest) =>
    postJson<import('../types/database').QueryResponse>('/database/query', data),
  updateRow: (data: import('../types/database').UpdateRowRequest) =>
    postJson('/database/table/update', data),
  deleteRow: (data: import('../types/database').DeleteRowRequest) =>
    postJson('/database/table/delete', data),
  insertRow: (data: import('../types/database').InsertRowRequest) =>
    postJson('/database/table/insert', data),
};
