export interface DashboardMetrics {
  totalMachines: number;
  activeMachines: number;
  openIncidents: number;
  activeWorkOrders: number;
  availabilityPercentage: number;
  mttrMinutes: number;
}

export interface IncidentTrendPoint {
  date: string;
  incidents: number;
}

export type IncidentPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type IncidentStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CANCELLED';
export type UserRole = 'ADMIN' | 'TECHNICIAN' | 'OPERATOR';
export type MachineStatus = 'RUNNING' | 'STOPPED' | 'MAINTENANCE' | 'INACTIVE';
export type MaintenanceType = 'CORRECTIVE' | 'PREVENTIVE' | 'INSPECTION';
export type WorkOrderPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type WorkOrderStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type AlertSeverity = 'CRITICAL' | 'WARNING';

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

export interface Machine {
  id: string;
  productionLineId: string;
  productionLine: string;
  name: string;
  assetCode: string;
  manufacturer: string | null;
  model: string | null;
  serialNumber: string | null;
  status: MachineStatus;
  installedAt: string | null;
}

export interface ProductionLine {
  id: string;
  name: string;
  code: string;
  active: boolean;
}

export interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
  demoAccount: boolean;
}

export interface WorkOrder {
  id: string;
  machineId: string;
  assetCode: string;
  machineName: string;
  incidentId: string | null;
  title: string;
  description: string;
  type: MaintenanceType;
  priority: WorkOrderPriority;
  status: WorkOrderStatus;
  scheduledFor: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface Downtime {
  id: string;
  machineId: string;
  assetCode: string;
  machineName: string;
  incidentId: string | null;
  reason: string;
  startedAt: string;
  endedAt: string | null;
  createdAt: string;
}

export interface AuditEvent {
  id: string;
  action: string;
  entityType: string;
  entityId: string;
  description: string;
  actorEmail: string;
  createdAt: string;
}

export interface OperationalAlert {
  key: string;
  severity: AlertSeverity;
  title: string;
  message: string;
  sourceType: string;
  sourceId: string;
  detectedAt: string;
}

export interface CreateIncidentInput {
  machineId: string;
  title: string;
  description: string;
  priority: IncidentPriority;
  occurredAt?: string;
}

export interface CreateWorkOrderInput {
  machineId: string;
  incidentId?: string;
  title: string;
  description: string;
  type: MaintenanceType;
  priority: WorkOrderPriority;
  scheduledFor?: string;
}

export interface CreateDowntimeInput {
  machineId: string;
  incidentId?: string;
  reason: string;
  startedAt?: string;
}

export interface CreateMachineInput {
  productionLineId: string;
  name: string;
  assetCode: string;
  manufacturer?: string;
  model?: string;
  serialNumber?: string;
  status: MachineStatus;
  installedAt?: string;
}

export interface UpdateMachineInput {
  productionLineId: string;
  name: string;
  assetCode: string;
  manufacturer?: string;
  model?: string;
  serialNumber?: string;
  installedAt?: string;
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
