export type OrganizationType = 'COMPANY' | 'SERVICE_PROVIDER';
export type OrganizationRole = 'OWNER' | 'ADMIN' | 'TECHNICIAN' | 'OPERATOR' | 'MECHANIC';
export type ServiceRelationshipStatus = 'ACTIVE' | 'SUSPENDED';
export type ServiceRequestChannel = 'INTERNAL' | 'EXTERNAL';
export type ServiceRequestPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ServiceRequestStatus = 'REQUESTED' | 'ACCEPTED' | 'EN_ROUTE' | 'IN_PROGRESS' | 'COMPLETED' | 'APPROVED' | 'DECLINED' | 'CANCELLED';

export interface OrganizationSummary {
  id: string;
  name: string;
  slug: string;
  type: OrganizationType;
  role: OrganizationRole;
}

export interface ProviderOrganization {
  id: string;
  name: string;
  slug: string;
}

export interface ServiceRelationship {
  id: string;
  companyOrganizationId: string;
  companyName: string;
  providerOrganizationId: string;
  providerName: string;
  status: ServiceRelationshipStatus;
  createdAt: string;
}

export interface OrganizationMember {
  membershipId: number;
  userId: string;
  name: string;
  registration: string;
  role: OrganizationRole;
  active: boolean;
}

export interface ServiceRequest {
  id: string;
  companyOrganizationId: string;
  companyName: string;
  providerOrganizationId: string | null;
  providerName: string | null;
  machineId: string;
  machineName: string;
  machineAssetCode: string;
  incidentId: string | null;
  incidentTitle: string | null;
  workOrderId: string | null;
  assignedTechnicianId: string | null;
  assignedTechnicianName: string | null;
  title: string;
  description: string;
  channel: ServiceRequestChannel;
  priority: ServiceRequestPriority;
  status: ServiceRequestStatus;
  eta: string | null;
  serviceNotes: string | null;
  partsUsed: string | null;
  declineReason: string | null;
  requestedAt: string;
  acceptedAt: string | null;
  enRouteAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  approvedAt: string | null;
  declinedAt: string | null;
  cancelledAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface CreateServiceRequestInput {
  machineId: string;
  incidentId?: string;
  title: string;
  description: string;
  channel: ServiceRequestChannel;
  priority: ServiceRequestPriority;
  providerOrganizationId?: string;
}

export interface ProviderOnboardingInput {
  name: string;
  slug: string;
  ownerName: string;
  ownerRegistration: string;
  ownerPassword: string;
}

export interface ProviderOnboardingResponse {
  provider: ProviderOrganization;
  relationship: ServiceRelationship;
  ownerRegistration: string;
}
