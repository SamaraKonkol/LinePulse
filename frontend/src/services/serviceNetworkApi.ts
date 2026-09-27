import api from './api';
import type { CreateServiceRequestInput, OrganizationMember, OrganizationRole, OrganizationSummary, PageResponse, ProviderOnboardingInput, ProviderOnboardingResponse, ProviderOrganization, ServiceRelationship, ServiceRequest, ServiceRequestStatus } from '../types/serviceNetwork';

export const ACTIVE_ORGANIZATION_STORAGE_KEY = 'linepulse-active-organization-v3';

export function setActiveOrganization(organizationId: string) {
  localStorage.setItem(ACTIVE_ORGANIZATION_STORAGE_KEY, organizationId);
}

export function getActiveOrganizationId() {
  return localStorage.getItem(ACTIVE_ORGANIZATION_STORAGE_KEY);
}

export function clearActiveOrganization() {
  localStorage.removeItem(ACTIVE_ORGANIZATION_STORAGE_KEY);
}

export async function getMyOrganizations() {
  return (await api.get<OrganizationSummary[]>('/organizations/my')).data;
}

export async function getCurrentOrganization() {
  return (await api.get<OrganizationSummary>('/organizations/current')).data;
}

export async function getServiceRequests(status?: ServiceRequestStatus) {
  return (await api.get<PageResponse<ServiceRequest>>('/service-requests', { params: { page: 0, size: 100, ...(status ? { status } : {}) } })).data;
}

export async function createServiceRequest(input: CreateServiceRequestInput) {
  return (await api.post<ServiceRequest>('/service-requests', input)).data;
}

export async function acceptServiceRequest(id: string, eta?: string) {
  return (await api.patch<ServiceRequest>(`/service-requests/${id}/accept`, { eta })).data;
}

export async function declineServiceRequest(id: string, reason: string) {
  return (await api.patch<ServiceRequest>(`/service-requests/${id}/decline`, { reason })).data;
}

export async function assignServiceRequest(id: string, technicianId: string) {
  return (await api.patch<ServiceRequest>(`/service-requests/${id}/assign`, { technicianId })).data;
}

export async function updateServiceRequestEta(id: string, eta: string) {
  return (await api.patch<ServiceRequest>(`/service-requests/${id}/eta`, { eta })).data;
}

export async function markServiceRequestEnRoute(id: string) {
  return (await api.patch<ServiceRequest>(`/service-requests/${id}/en-route`)).data;
}

export async function startServiceRequest(id: string) {
  return (await api.patch<ServiceRequest>(`/service-requests/${id}/start`)).data;
}

export async function completeServiceRequest(id: string, serviceNotes: string, partsUsed?: string) {
  return (await api.patch<ServiceRequest>(`/service-requests/${id}/complete`, { serviceNotes, partsUsed })).data;
}

export async function approveServiceRequest(id: string) {
  return (await api.patch<ServiceRequest>(`/service-requests/${id}/approve`)).data;
}

export async function cancelServiceRequest(id: string) {
  return (await api.patch<ServiceRequest>(`/service-requests/${id}/cancel`)).data;
}

export async function getProviders() {
  return (await api.get<ProviderOrganization[]>('/provider-network/providers')).data;
}

export async function getProviderRelationships() {
  return (await api.get<ServiceRelationship[]>('/provider-network/relationships')).data;
}

export async function trustProvider(providerOrganizationId: string) {
  return (await api.post<ServiceRelationship>('/provider-network/relationships', { providerOrganizationId })).data;
}

export async function suspendProviderRelationship(id: string) {
  return (await api.patch<ServiceRelationship>(`/provider-network/relationships/${id}/suspend`)).data;
}

export async function onboardProvider(input: ProviderOnboardingInput) {
  return (await api.post<ProviderOnboardingResponse>('/provider-network/providers', input)).data;
}

export async function getOrganizationMembers() {
  return (await api.get<OrganizationMember[]>('/organization-members')).data;
}

export async function createOrganizationMember(input: { name: string; registration: string; password: string; role: OrganizationRole }) {
  return (await api.post<OrganizationMember>('/organization-members', input)).data;
}

export async function updateOrganizationMemberRole(userId: string, role: OrganizationRole) {
  return (await api.patch<OrganizationMember>(`/organization-members/${userId}/role`, { role })).data;
}

export async function updateOrganizationMemberStatus(userId: string, active: boolean) {
  return (await api.patch<OrganizationMember>(`/organization-members/${userId}/status`, { active })).data;
}
