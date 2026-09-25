import axios from 'axios';
import type { DashboardMetrics, Incident } from '../types/api';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
  timeout: 8000,
});

export async function getDashboardMetrics() {
  const response = await api.get<DashboardMetrics>('/dashboard');
  return response.data;
}

export async function getIncidents() {
  const response = await api.get<Incident[]>('/incidents');
  return response.data;
}

export default api;
