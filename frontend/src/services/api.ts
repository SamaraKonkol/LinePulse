import axios from 'axios';
import type { AuthResponse, CreateIncidentInput, CreateWorkOrderInput, DashboardMetrics, Incident, Machine, MachineStatus, WorkOrder } from '../types/api';

const AUTH_STORAGE_KEY = 'linepulse-auth';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
  timeout: 8000,
});

api.interceptors.request.use((config) => {
  const session = getStoredAuth();
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

export function getStoredAuth(): AuthResponse | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return null;

  try {
    return JSON.parse(raw) as AuthResponse;
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function storeAuth(auth: AuthResponse) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
}

export function clearAuth() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export async function login(email: string, password: string) {
  const response = await api.post<AuthResponse>('/auth/login', { email, password });
  return response.data;
}

export async function register(name: string, email: string, password: string) {
  const response = await api.post<AuthResponse>('/auth/register', { name, email, password });
  return response.data;
}

export async function getDashboardMetrics() {
  const response = await api.get<DashboardMetrics>('/dashboard');
  return response.data;
}

export async function getIncidents() {
  const response = await api.get<Incident[]>('/incidents');
  return response.data;
}

export async function getMachines() {
  const response = await api.get<Machine[]>('/machines');
  return response.data;
}

export async function updateMachineStatus(machineId: string, status: MachineStatus) {
  const response = await api.patch<Machine>(`/machines/${machineId}/status`, { status });
  return response.data;
}

export async function createIncident(input: CreateIncidentInput) {
  const response = await api.post<Incident>('/incidents', input);
  return response.data;
}

export async function getWorkOrders() {
  const response = await api.get<WorkOrder[]>('/work-orders');
  return response.data;
}

export async function createWorkOrder(input: CreateWorkOrderInput) {
  const response = await api.post<WorkOrder>('/work-orders', input);
  return response.data;
}

export default api;
