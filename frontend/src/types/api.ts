export interface DashboardMetrics {
  totalMachines: number;
  activeMachines: number;
  openIncidents: number;
  activeWorkOrders: number;
  availabilityPercentage: number;
}

export type IncidentPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type IncidentStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CANCELLED';
export type UserRole = 'ADMIN' | 'TECHNICIAN' | 'OPERATOR';

export interface Incident {
  id: string;
  machineId: string;
  assetCode: string;
  machineName: string;
  title: string;
  description: string;
  priority: IncidentPriority;
  status: IncidentStatus;
  occurredAt: string;
  createdAt: string;
}

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  user: AuthUser;
}
