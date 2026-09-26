package com.linepulse.asset;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLineRepository extends JpaRepository<ProductionLine, UUID> {
    List<ProductionLine> findAllBySectorId(UUID sectorId);
    boolean existsBySectorIdAndCodeIgnoreCase(UUID sectorId, String code);
    boolean existsBySectorIdAndCodeIgnoreCaseAndIdNot(UUID sectorId, String code, UUID id);
}
