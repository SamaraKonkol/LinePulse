package com.linepulse.asset;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLineRepository extends JpaRepository<ProductionLine, UUID> {
}
