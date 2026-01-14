export interface PreferenceEntry {
  key: string;
  type: string;
  value: string;
}

export interface PreferenceFile {
  name: string;
  entries: PreferenceEntry[];
}

export interface PreferencesResponse {
  preferences: PreferenceFile[];
  total: number;
}
