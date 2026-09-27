package com.linepulse.asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<Machine, UUID> {
    List<Machine> findAllByProductionLine_Sector_Plant_Organization_Id(UUID organizationId);
    Optional<Machine> findByIdAndProductionLine_Sector_Plant_Organization_Id(UUID id, UUID organizationId);
    boolean existsByProductionLine_Sector_Plant_Organization_IdAndAssetCodeIgnoreCase(UUID organizationId, String assetCode);
    boolean existsByProductionLine_Sector_Plant_Organization_IdAndAssetCodeIgnoreCaseAndIdNot(UUID organizationId, String assetCode, UUID id);
    long countByProductionLine_Sector_Plant_Organization_IdAndStatus(UUID organizationId, MachineStatus status);
    long countByProductionLine_Sector_Plant_Organization_IdAndStatusNot(UUID organizationId, MachineStatus status);
}
