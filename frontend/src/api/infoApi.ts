import axios from 'axios';

export interface AppInfo {
  build?: {
    artifact?: string;
    name?: string;
    version?: string;
    time?: string;
  };
  llm?: {
    provider?: string;
    model?: string;
  };
  os?: {
    name?: string;
    version?: string;
    arch?: string;
  };
  java?: {
    version?: string;
    vendor?: string;
  };
}

export async function getAppInfo(): Promise<AppInfo> {
  const response = await axios.get<AppInfo>(`${import.meta.env.BASE_URL}actuator/info`);
  return response.data;
}
