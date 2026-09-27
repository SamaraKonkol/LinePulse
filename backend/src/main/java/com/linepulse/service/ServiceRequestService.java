package com.linepulse.service;

import com.linepulse.asset.Machine;
import com.linepulse.asset.MachineRepository;
import com.linepulse.auth.UserAccount;
import com.linepulse.auth.UserRepository;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import com.linepulse.common.PageResponse;
import com.linepulse.incident.Incident;
import com.linepulse.incident.IncidentRepository;
import com.linepulse.organization.Organization;
import com.linepulse.organization.OrganizationAccessService;
import com.linepulse.organization.OrganizationMembershipRepository;
import com.linepulse.organization.OrganizationRole;
import com.linepulse.organization.OrganizationType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceRequestService {
    private static final Set<OrganizationRole> TECHNICAL_ROLES = Set.of(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.TECHNICIAN,
            OrganizationRole.MECHANIC
    );

    private final OrganizationAccessService accessService;
    private final OrganizationMembershipRepository membershipRepository;
    private final ServiceRelationshipRepository relationshipRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final MachineRepository machineRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public ServiceRequestService(
            OrganizationAccessService accessService,
            OrganizationMembershipRepository membershipRepository,
            ServiceRelationshipRepository relationshipRepository,
            ServiceRequestRepository serviceRequestRepository,
            MachineRepository machineRepository,
            IncidentRepository incidentRepository,
            UserRepository userRepository
    ) {
        this.accessService = accessService;
        this.membershipRepository = membershipRepository;
        this.relationshipRepository = relationshipRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.machineRepository = machineRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ServiceRequestResponse create(CreateServiceRequestRequest request) {
        var membership = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.TECHNICIAN);
        Organization company = membership.getOrganization();
        if (company.getType() != OrganizationType.COMPANY) {
            throw new AccessDeniedException("Somente empresas podem abrir chamados de serviço.");
        }

        Machine machine = machineRepository.findByIdAndProductionLine_Sector_Plant_Organization_Id(request.machineId(), company.getId())
                .orElseThrow(() -> new NotFoundException("Máquina não encontrada nesta organização."));
        Incident incident = null;
        if (request.incidentId() != null) {
            incident = incidentRepository.findByIdAndMachine_ProductionLine_Sector_Plant_Organization_Id(request.incidentId(), company.getId())
                    .orElseThrow(() -> new NotFoundException("Ocorrência não encontrada nesta organização."));
            if (!incident.getMachine().getId().equals(machine.getId())) {
                throw new ConflictException("A ocorrência informada pertence a outra máquina.");
            }
        }

        Organization provider = null;
        if (request.channel() == ServiceRequestChannel.EXTERNAL) {
            if (request.providerOrganizationId() == null) {
                throw new ConflictException("Chamados externos exigem um prestador.");
            }
            ServiceRelationship relationship = relationshipRepository
                    .findByCompany_IdAndProvider_Id(company.getId(), request.providerOrganizationId())
                    .filter(candidate -> candidate.getStatus() == ServiceRelationshipStatus.ACTIVE)
                    .orElseThrow(() -> new AccessDeniedException("O prestador não está ativo na rede confiável desta empresa."));
            provider = relationship.getProvider();
        } else if (request.providerOrganizationId() != null) {
            throw new ConflictException("Chamados internos não devem informar prestador externo.");
        }

        Instant now = Instant.now();
        ServiceRequest serviceRequest = new ServiceRequest(
                UUID.randomUUID(),
                company,
                provider,
                machine,
                incident,
                request.title().trim(),
                request.description().trim(),
                request.channel(),
                request.priority(),
                now
        );
        return ServiceRequestResponse.from(serviceRequestRepository.save(serviceRequest));
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceRequestResponse> list(ServiceRequestStatus status, int page, int size) {
        var membership = accessService.currentMembership();
        Organization organization = membership.getOrganization();
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<ServiceRequest> result;
        if (organization.getType() == OrganizationType.COMPANY) {
            result = status == null
                    ? serviceRequestRepository.findByCompany_Id(organization.getId(), pageable)
                    : serviceRequestRepository.findByCompany_IdAndStatus(organization.getId(), status, pageable);
        } else {
            result = status == null
                    ? serviceRequestRepository.findByProvider_Id(organization.getId(), pageable)
                    : serviceRequestRepository.findByProvider_IdAndStatus(organization.getId(), status, pageable);
        }
        return PageResponse.from(result, result.getContent().stream().map(ServiceRequestResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse get(UUID id) {
        return ServiceRequestResponse.from(findVisible(id));
    }

    @Transactional
    public ServiceRequestResponse accept(UUID id, AcceptServiceRequestRequest request) {
        ServiceRequest serviceRequest = findForExecutor(id);
        requireStatus(serviceRequest, ServiceRequestStatus.REQUESTED);
        serviceRequest.accept(request.eta(), Instant.now());
        return ServiceRequestResponse.from(serviceRequestRepository.save(serviceRequest));
    }

    @Transactional
    public ServiceRequestResponse decline(UUID id, DeclineServiceRequestRequest request) {
        ServiceRequest serviceRequest = findForExecutor(id);
        requireStatus(serviceRequest, ServiceRequestStatus.REQUESTED);
        serviceRequest.decline(request.reason().trim(), Instant.now());
        return ServiceRequestResponse.from(serviceRequestRepository.save(serviceRequest));
    }

    @Transactional
    public ServiceRequestResponse assign(UUID id, AssignServiceRequestRequest request) {
        ServiceRequest serviceRequest = findForExecutor(id);
        if (Set.of(ServiceRequestStatus.COMPLETED, ServiceRequestStatus.APPROVED, ServiceRequestStatus.DECLINED, ServiceRequestStatus.CANCELLED).contains(serviceRequest.getStatus())) {
            throw new ConflictException("Chamado encerrado não pode receber técnico.");
        }
        Organization executorOrganization = serviceRequest.getChannel() == ServiceRequestChannel.EXTERNAL
                ? serviceRequest.getProvider()
                : serviceRequest.getCompany();
        UserAccount technician = userRepository.findById(request.technicianId())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new NotFoundException("Técnico não encontrado."));
        var technicianMembership = membershipRepository.findByOrganization_IdAndUser_RegistrationIgnoreCaseAndActiveTrue(executorOrganization.getId(), technician.getRegistration())
                .orElseThrow(() -> new AccessDeniedException("O técnico não pertence à organização responsável pelo chamado."));
        if (!TECHNICAL_ROLES.contains(technicianMembership.getRole())) {
            throw new AccessDeniedException("O usuário informado não possui papel técnico nesta organização.");
        }
        serviceRequest.assignTechnician(technician, Instant.now());
        return ServiceRequestResponse.from(serviceRequestRepository.save(serviceRequest));
    }

    @Transactional
    public ServiceRequestResponse updateEta(UUID id, UpdateServiceRequestEtaRequest request) {
        ServiceRequest serviceRequest = findForExecutor(id);
        if (!Set.of(ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.EN_ROUTE).contains(serviceRequest.getStatus())) {
            throw new ConflictException("A previsão só pode ser alterada após o aceite e antes do início do serviço.");
        }
        serviceRequest.updateEta(request.eta(), Instant.now());
        return ServiceRequestResponse.from(serviceRequestRepository.save(serviceRequest));
    }

    @Transactional
    public ServiceRequestResponse markEnRoute(UUID id) {
        ServiceRequest serviceRequest = findForExecutor(id);
        if (serviceRequest.getChannel() != ServiceRequestChannel.EXTERNAL) {
            throw new ConflictException("O status a caminho só se aplica a atendimento externo.");
        }
        requireStatus(serviceRequest, ServiceRequestStatus.ACCEPTED);
        serviceRequest.markEnRoute(Instant.now());
        return ServiceRequestResponse.from(serviceRequestRepository.save(serviceRequest));
    }

    @Transactional
    public ServiceRequestResponse start(UUID id) {
        ServiceRequest serviceRequest = findForExecutor(id);
        if (!Set.of(ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.EN_ROUTE).contains(serviceRequest.getStatus())) {
            throw new ConflictException("O serviço só pode iniciar após o aceite.");
        }
        serviceRequest.start(Instant.now());
        return ServiceRequestResponse.from(serviceRequestRepository.save(serviceRequest));
    }

    @Transactional
    public ServiceRequestResponse complete(UUID id, CompleteServiceRequestRequest request) {
        ServiceRequest serviceRequest = findForExecutor(id);
        requireStatus(serviceRequest, ServiceRequestStatus.IN_PROGRESS);
        serviceRequest.complete(request.serviceNotes().trim(), normalize(request.partsUsed()), Instant.now());
        return ServiceRequestResponse.from(serviceRequestRepository.save(serviceRequest));
    }

    @Transactional
    public ServiceRequestResponse approve(UUID id) {
        var membership = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        ServiceRequest serviceRequest = serviceRequestRepository.findByIdAndCompany_Id(id, membership.getOrganization().getId())
                .orElseThrow(() -> new NotFoundException("Chamado não encontrado."));
        requireStatus(serviceRequest, ServiceRequestStatus.COMPLETED);
        serviceRequest.approve(Instant.now());
        return ServiceRequestResponse.from(serviceRequestRepository.save(serviceRequest));
    }

    @Transactional
    public ServiceRequestResponse cancel(UUID id) {
        var membership = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.TECHNICIAN);
        ServiceRequest serviceRequest = serviceRequestRepository.findByIdAndCompany_Id(id, membership.getOrganization().getId())
                .orElseThrow(() -> new NotFoundException("Chamado não encontrado."));
        if (!Set.of(ServiceRequestStatus.REQUESTED, ServiceRequestStatus.ACCEPTED).contains(serviceRequest.getStatus())) {
            throw new ConflictException("Somente chamados aguardando atendimento ou recém-aceitos podem ser cancelados.");
        }
        serviceRequest.cancel(Instant.now());
        return ServiceRequestResponse.from(serviceRequestRepository.save(serviceRequest));
    }

    private ServiceRequest findVisible(UUID id) {
        var membership = accessService.currentMembership();
        Organization organization = membership.getOrganization();
        return (organization.getType() == OrganizationType.COMPANY
                ? serviceRequestRepository.findByIdAndCompany_Id(id, organization.getId())
                : serviceRequestRepository.findByIdAndProvider_Id(id, organization.getId()))
                .orElseThrow(() -> new NotFoundException("Chamado não encontrado."));
    }

    private ServiceRequest findForExecutor(UUID id) {
        var membership = accessService.currentMembership();
        Organization organization = membership.getOrganization();
        if (!TECHNICAL_ROLES.contains(membership.getRole())) {
            throw new AccessDeniedException("Seu papel nesta organização não permite executar chamados.");
        }
        if (organization.getType() == OrganizationType.SERVICE_PROVIDER) {
            return serviceRequestRepository.findByIdAndProvider_Id(id, organization.getId())
                    .orElseThrow(() -> new NotFoundException("Chamado não encontrado."));
        }
        ServiceRequest request = serviceRequestRepository.findByIdAndCompany_Id(id, organization.getId())
                .orElseThrow(() -> new NotFoundException("Chamado não encontrado."));
        if (request.getChannel() != ServiceRequestChannel.INTERNAL) {
            throw new AccessDeniedException("Chamados externos são executados pelo prestador selecionado.");
        }
        return request;
    }

    private void requireStatus(ServiceRequest request, ServiceRequestStatus expected) {
        if (request.getStatus() != expected) {
            throw new ConflictException("Transição inválida para o estado atual do chamado.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
