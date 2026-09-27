package com.linepulse.asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLineRepository extends JpaRepository<ProductionLine, UUID> {
    List<ProductionLine> findAllBySectorId(UUID sectorId);
    List<ProductionLine> findAllBySector_Plant_Organization_Id(UUID organizationId);
    Optional<ProductionLine> findByIdAndSector_Plant_Organization_Id(UUID id, UUID organizationId);
    boolean existsBySectorIdAndCodeIgnoreCase(UUID sectorId, String code);
    boolean existsBySectorIdAndCodeIgnoreCaseAndIdNot(UUID sectorId, String code, UUID id);
}
