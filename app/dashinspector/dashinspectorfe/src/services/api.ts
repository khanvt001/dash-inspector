const API_BASE_URL = 'http://localhost:8080/api';

export async function fetchJson<T>(endpoint: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`);
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  return response.json();
}

export const preferencesApi = {
  getAll: () => fetchJson<import('../types/preferences').PreferencesResponse>('/preferences'),
};
