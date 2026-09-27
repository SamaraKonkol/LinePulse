import axios from 'axios';
import type { AdminUser, AuditEvent, AuthResponse, CreateDowntimeInput, CreateIncidentInput, CreateMachineInput, CreateWorkOrderInput, DashboardMetrics, Downtime, Incident, IncidentTrendPoint, Machine, MachineStatus, MaintenancePlan, MaintenancePlanInput, OperationalAlert, Plant, ProductionLine, ResolveIncidentInput, Sector, UpdateMachineInput, UserRole, WorkOrder } from '../types/api';

const AUTH_STORAGE_KEY = 'linepulse-auth-v2';
const LEGACY_AUTH_STORAGE_KEY = 'linepulse-auth';
const API_BASE_URL = import.meta.env.VITE_API_URL?.trim() || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 65000,
});

api.interceptors.request.use((config) => {
  const session = getStoredAuth();
  if (session?.token) config.headers.Authorization = `Bearer ${session.token}`;
  return config;
});

export function getStoredAuth(): AuthResponse | null {
  localStorage.removeItem(LEGACY_AUTH_STORAGE_KEY);
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return null;
  try { return JSON.parse(raw) as AuthResponse; } catch { localStorage.removeItem(AUTH_STORAGE_KEY); return null; }
}

export function storeAuth(auth: AuthResponse) { localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth)); }
export function clearAuth() { localStorage.removeItem(AUTH_STORAGE_KEY); localStorage.removeItem(LEGACY_AUTH_STORAGE_KEY); }

export async function login(registration: string, password: string) { return (await api.post<AuthResponse>('/auth/login', { registration, password })).data; }
export async function getDashboardMetrics() { return (await api.get<DashboardMetrics>('/dashboard')).data; }
export async function getIncidentTrend() { return (await api.get<IncidentTrendPoint[]>('/dashboard/incident-trend')).data; }
export async function getAlerts() { return (await api.get<OperationalAlert[]>('/alerts')).data; }
export async function getAuditEvents() { return (await api.get<AuditEvent[]>('/audit-events')).data; }
export async function getIncidents() { return (await api.get<Incident[]>('/incidents')).data; }
export async function getMachines() { return (await api.get<Machine[]>('/machines')).data; }
export async function createMachine(input: CreateMachineInput) { return (await api.post<Machine>('/machines', input)).data; }
export async function updateMachine(machineId: string, input: UpdateMachineInput) { return (await api.patch<Machine>(`/machines/${machineId}`, input)).data; }
export async function updateMachineStatus(machineId: string, status: MachineStatus) { return (await api.patch<Machine>(`/machines/${machineId}/status`, { status })).data; }

export async function getAdminUsers() { return (await api.get<AdminUser[]>('/admin/users')).data; }
export async function createAdminUser(input: { name: string; registration: string; password: string; role: UserRole }) { return (await api.post<AdminUser>('/admin/users', input)).data; }
export async function updateAdminUserRole(userId: string, role: UserRole) { return (await api.patch<AdminUser>(`/admin/users/${userId}/role`, { role })).data; }
export async function updateAdminUserStatus(userId: string, active: boolean) { return (await api.patch<AdminUser>(`/admin/users/${userId}/status`, { active })).data; }
export async function deleteAdminUser(userId: string) { await api.delete(`/admin/users/${userId}`); }

export async function getProductionLines() { return (await api.get<ProductionLine[]>('/admin/production-lines')).data; }
export async function getPlants() { return (await api.get<Plant[]>('/admin/structure/plants')).data; }
export async function createPlant(input: { name: string; code: string }) { return (await api.post<Plant>('/admin/structure/plants', input)).data; }
export async function updatePlant(id: string, input: { name: string; code: string }) { return (await api.patch<Plant>(`/admin/structure/plants/${id}`, input)).data; }
export async function updatePlantStatus(id: string, active: boolean) { return (await api.patch<Plant>(`/admin/structure/plants/${id}/status`, { active })).data; }
export async function getSectors() { return (await api.get<Sector[]>('/admin/structure/sectors')).data; }
export async function createSector(input: { plantId: string; name: string; code: string }) { return (await api.post<Sector>('/admin/structure/sectors', input)).data; }
export async function updateSector(id: string, input: { plantId: string; name: string; code: string }) { return (await api.patch<Sector>(`/admin/structure/sectors/${id}`, input)).data; }
export async function updateSectorStatus(id: string, active: boolean) { return (await api.patch<Sector>(`/admin/structure/sectors/${id}/status`, { active })).data; }
export async function getStructureLines() { return (await api.get<ProductionLine[]>('/admin/structure/lines')).data; }
export async function createProductionLine(input: { sectorId: string; name: string; code: string }) { return (await api.post<ProductionLine>('/admin/structure/lines', input)).data; }
export async function updateProductionLine(id: string, input: { sectorId: string; name: string; code: string }) { return (await api.patch<ProductionLine>(`/admin/structure/lines/${id}`, input)).data; }
export async function updateProductionLineStatus(id: string, active: boolean) { return (await api.patch<ProductionLine>(`/admin/structure/lines/${id}/status`, { active })).data; }

export async function createIncident(input: CreateIncidentInput) { return (await api.post<Incident>('/incidents', input)).data; }
export async function startIncident(id: string) { return (await api.patch<Incident>(`/incidents/${id}/start`, {})).data; }
export async function resolveIncident(id: string, input: ResolveIncidentInput) { return (await api.patch<Incident>(`/incidents/${id}/resolve`, input)).data; }
export async function cancelIncident(id: string) { return (await api.patch<Incident>(`/incidents/${id}/cancel`, {})).data; }

export async function getWorkOrders() { return (await api.get<WorkOrder[]>('/work-orders')).data; }
export async function createWorkOrder(input: CreateWorkOrderInput) { return (await api.post<WorkOrder>('/work-orders', input)).data; }
export async function startWorkOrder(id: string) { return (await api.patch<WorkOrder>(`/work-orders/${id}/start`)).data; }
export async function completeWorkOrder(id: string) { return (await api.patch<WorkOrder>(`/work-orders/${id}/complete`)).data; }

export async function getMaintenancePlans() { return (await api.get<MaintenancePlan[]>('/maintenance-plans')).data; }
export async function createMaintenancePlan(input: MaintenancePlanInput) { return (await api.post<MaintenancePlan>('/maintenance-plans', input)).data; }
export async function updateMaintenancePlan(id: string, input: MaintenancePlanInput) { return (await api.put<MaintenancePlan>(`/maintenance-plans/${id}`, input)).data; }
export async function updateMaintenancePlanStatus(id: string, active: boolean) { return (await api.patch<MaintenancePlan>(`/maintenance-plans/${id}/status`, { active })).data; }
export async function generateMaintenancePlan(id: string) { return (await api.post<MaintenancePlan>(`/maintenance-plans/${id}/generate`)).data; }

export async function getDowntimes() { return (await api.get<Downtime[]>('/downtimes')).data; }
export async function createDowntime(input: CreateDowntimeInput) { return (await api.post<Downtime>('/downtimes', input)).data; }
export async function closeDowntime(id: string) { return (await api.patch<Downtime>(`/downtimes/${id}/close`, {})).data; }

export default api;
