package com.linepulse.asset;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/production-lines")
public class ProductionLineController {
    private final IndustrialStructureService structureService;

    public ProductionLineController(IndustrialStructureService structureService) {
        this.structureService = structureService;
    }

    @GetMapping
    List<ProductionLineResponse> findAll() {
        return structureService.findLines();
    }
}
