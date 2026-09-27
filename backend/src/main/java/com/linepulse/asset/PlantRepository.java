package com.linepulse.asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantRepository extends JpaRepository<Plant, UUID> {
    List<Plant> findAllByOrganization_Id(UUID organizationId);
    Optional<Plant> findByIdAndOrganization_Id(UUID id, UUID organizationId);
    boolean existsByOrganization_IdAndCodeIgnoreCase(UUID organizationId, String code);
    boolean existsByOrganization_IdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);
}
