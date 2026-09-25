package com.linepulse.asset;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<Machine, UUID> {
    boolean existsByAssetCodeIgnoreCase(String assetCode);
    long countByStatus(MachineStatus status);
    long countByStatusNot(MachineStatus status);
}
