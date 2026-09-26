package com.linepulse.asset;

import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/production-lines")
public class ProductionLineController {
    private final ProductionLineRepository productionLineRepository;

    public ProductionLineController(ProductionLineRepository productionLineRepository) {
        this.productionLineRepository = productionLineRepository;
    }

    @GetMapping
    List<ProductionLineResponse> findAll() {
        return productionLineRepository.findAll().stream()
                .sorted(Comparator.comparing(ProductionLine::getName, String.CASE_INSENSITIVE_ORDER))
                .map(ProductionLineResponse::from)
                .toList();
    }
}
