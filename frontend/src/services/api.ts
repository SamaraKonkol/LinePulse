import axios from 'axios';
import type { AdminUser, AuditEvent, AuthResponse, CreateDowntimeInput, CreateIncidentInput, CreateMachineInput, CreateWorkOrderInput, DashboardMetrics, Downtime, Incident, IncidentTrendPoint, Machine, MachineStatus, OperationalAlert, ProductionLine, UpdateMachineInput, UserRole, WorkOrder } from '../types/api';

const AUTH_STORAGE_KEY = 'linepulse-auth';
const API_BASE_URL = import.meta.env.VITE_API_URL?.trim() || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
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

export function storeAuth(auth: AuthResponse) { localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth)); }
export function clearAuth() { localStorage.removeItem(AUTH_STORAGE_KEY); }

export async function login(email: string, password: string) {
  const response = await api.post<AuthResponse>('/auth/login', { email, password });
  return response.data;
}

export async function getDashboardMetrics() {
  const response = await api.get<DashboardMetrics>('/dashboard');
  return response.data;
}

export async function getIncidentTrend() {
  const response = await api.get<IncidentTrendPoint[]>('/dashboard/incident-trend');
  return response.data;
}

export async function getAlerts() {
  const response = await api.get<OperationalAlert[]>('/alerts');
  return response.data;
}

export async function getAuditEvents() {
  const response = await api.get<AuditEvent[]>('/audit-events');
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

export async function createMachine(input: CreateMachineInput) {
  const response = await api.post<Machine>('/machines', input);
  return response.data;
}

export async function updateMachine(machineId: string, input: UpdateMachineInput) {
  const response = await api.patch<Machine>(`/machines/${machineId}`, input);
  return response.data;
}

export async function updateMachineStatus(machineId: string, status: MachineStatus) {
  const response = await api.patch<Machine>(`/machines/${machineId}/status`, { status });
  return response.data;
}

export async function getAdminUsers() {
  const response = await api.get<AdminUser[]>('/admin/users');
  return response.data;
}

export async function createAdminUser(input: { name: string; email: string; password: string; role: UserRole }) {
  const response = await api.post<AdminUser>('/admin/users', input);
  return response.data;
}

export async function updateAdminUserRole(userId: string, role: UserRole) {
  const response = await api.patch<AdminUser>(`/admin/users/${userId}/role`, { role });
  return response.data;
}

export async function updateAdminUserStatus(userId: string, active: boolean) {
  const response = await api.patch<AdminUser>(`/admin/users/${userId}/status`, { active });
  return response.data;
}

export async function getProductionLines() {
  const response = await api.get<ProductionLine[]>('/admin/production-lines');
  return response.data;
}

export async function createIncident(input: CreateIncidentInput) {
  const response = await api.post<Incident>('/incidents', input);
  return response.data;
}

export async function startIncident(id: string) {
  const response = await api.patch<Incident>(`/incidents/${id}/start`, {});
  return response.data;
}

export async function resolveIncident(id: string) {
  const response = await api.patch<Incident>(`/incidents/${id}/resolve`, {});
  return response.data;
}

export async function cancelIncident(id: string) {
  const response = await api.patch<Incident>(`/incidents/${id}/cancel`, {});
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

export async function startWorkOrder(id: string) {
  const response = await api.patch<WorkOrder>(`/work-orders/${id}/start`);
  return response.data;
}

export async function completeWorkOrder(id: string) {
  const response = await api.patch<WorkOrder>(`/work-orders/${id}/complete`);
  return response.data;
}

export async function getDowntimes() {
  const response = await api.get<Downtime[]>('/downtimes');
  return response.data;
}

export async function createDowntime(input: CreateDowntimeInput) {
  const response = await api.post<Downtime>('/downtimes', input);
  return response.data;
}

export async function closeDowntime(id: string) {
  const response = await api.patch<Downtime>(`/downtimes/${id}/close`, {});
  return response.data;
}

export default api;
