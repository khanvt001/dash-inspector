const API_BASE_URL = 'http://localhost:8080/api';

export async function fetchJson<T>(endpoint: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`);
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
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
    throw new Error(`HTTP error! status: ${response.status}`);
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
