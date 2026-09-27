package com.linepulse.asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, UUID> {
    List<Sector> findAllByPlantId(UUID plantId);
    List<Sector> findAllByPlant_Organization_Id(UUID organizationId);
    Optional<Sector> findByIdAndPlant_Organization_Id(UUID id, UUID organizationId);
    boolean existsByPlantIdAndCodeIgnoreCase(UUID plantId, String code);
    boolean existsByPlantIdAndCodeIgnoreCaseAndIdNot(UUID plantId, String code, UUID id);
}
