package com.linepulse.asset;

import com.linepulse.audit.AuditService;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndustrialStructureService {
    private final PlantRepository plantRepository;
    private final SectorRepository sectorRepository;
    private final ProductionLineRepository productionLineRepository;
    private final AuditService auditService;

    public IndustrialStructureService(
            PlantRepository plantRepository,
            SectorRepository sectorRepository,
            ProductionLineRepository productionLineRepository,
            AuditService auditService
    ) {
        this.plantRepository = plantRepository;
        this.sectorRepository = sectorRepository;
        this.productionLineRepository = productionLineRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PlantResponse> findPlants() {
        return plantRepository.findAll().stream()
                .sorted(Comparator.comparing(Plant::getName, String.CASE_INSENSITIVE_ORDER))
                .map(PlantResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SectorResponse> findSectors() {
        return sectorRepository.findAll().stream()
                .sorted(Comparator.comparing(Sector::getName, String.CASE_INSENSITIVE_ORDER))
                .map(SectorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionLineResponse> findLines() {
        return productionLineRepository.findAll().stream()
                .sorted(Comparator.comparing(ProductionLine::getName, String.CASE_INSENSITIVE_ORDER))
                .map(ProductionLineResponse::from)
                .toList();
    }

    @Transactional
    public PlantResponse createPlant(CreatePlantRequest request) {
        String code = normalizeCode(request.code());
        if (plantRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Já existe uma planta com este código.");
        }
        Plant saved = plantRepository.save(new Plant(UUID.randomUUID(), request.name().trim(), code, true, Instant.now()));
        auditService.record("PLANT_CREATED", "PLANT", saved.getId(), "Planta " + saved.getCode() + " cadastrada");
        return PlantResponse.from(saved);
    }

    @Transactional
    public PlantResponse updatePlant(UUID id, UpdatePlantRequest request) {
        Plant plant = findPlant(id);
        String code = normalizeCode(request.code());
        if (plantRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("Já existe uma planta com este código.");
        }
        plant.update(request.name().trim(), code);
        auditService.record("PLANT_UPDATED", "PLANT", plant.getId(), "Planta " + plant.getCode() + " atualizada");
        return PlantResponse.from(plant);
    }

    @Transactional
    public PlantResponse updatePlantStatus(UUID id, UpdateStructureStatusRequest request) {
        Plant plant = findPlant(id);
        plant.changeActive(request.active());
        auditService.record("PLANT_STATUS_CHANGED", "PLANT", plant.getId(), "Planta " + plant.getCode() + (request.active() ? " ativada" : " desativada"));
        return PlantResponse.from(plant);
    }

    @Transactional
    public SectorResponse createSector(CreateSectorRequest request) {
        Plant plant = findPlant(request.plantId());
        if (!plant.isActive()) throw new ConflictException("Ative a planta antes de cadastrar setores nela.");
        String code = normalizeCode(request.code());
        if (sectorRepository.existsByPlantIdAndCodeIgnoreCase(plant.getId(), code)) {
            throw new ConflictException("Já existe um setor com este código nesta planta.");
        }
        Sector saved = sectorRepository.save(new Sector(UUID.randomUUID(), plant, request.name().trim(), code, true, Instant.now()));
        auditService.record("SECTOR_CREATED", "SECTOR", saved.getId(), "Setor " + saved.getCode() + " cadastrado em " + plant.getCode());
        return SectorResponse.from(saved);
    }

    @Transactional
    public SectorResponse updateSector(UUID id, UpdateSectorRequest request) {
        Sector sector = findSector(id);
        Plant plant = findPlant(request.plantId());
        String code = normalizeCode(request.code());
        if (sectorRepository.existsByPlantIdAndCodeIgnoreCaseAndIdNot(plant.getId(), code, id)) {
            throw new ConflictException("Já existe um setor com este código nesta planta.");
        }
        sector.update(plant, request.name().trim(), code);
        auditService.record("SECTOR_UPDATED", "SECTOR", sector.getId(), "Setor " + sector.getCode() + " atualizado");
        return SectorResponse.from(sector);
    }

    @Transactional
    public SectorResponse updateSectorStatus(UUID id, UpdateStructureStatusRequest request) {
        Sector sector = findSector(id);
        if (request.active() && !sector.getPlant().isActive()) {
            throw new ConflictException("Ative a planta antes de ativar este setor.");
        }
        sector.changeActive(request.active());
        auditService.record("SECTOR_STATUS_CHANGED", "SECTOR", sector.getId(), "Setor " + sector.getCode() + (request.active() ? " ativado" : " desativado"));
        return SectorResponse.from(sector);
    }

    @Transactional
    public ProductionLineResponse createLine(CreateProductionLineRequest request) {
        Sector sector = findSector(request.sectorId());
        validateSectorHierarchy(sector);
        String code = normalizeCode(request.code());
        if (productionLineRepository.existsBySectorIdAndCodeIgnoreCase(sector.getId(), code)) {
            throw new ConflictException("Já existe uma linha com este código neste setor.");
        }
        ProductionLine saved = productionLineRepository.save(new ProductionLine(UUID.randomUUID(), sector, request.name().trim(), code, true, Instant.now()));
        auditService.record("LINE_CREATED", "PRODUCTION_LINE", saved.getId(), "Linha " + saved.getCode() + " cadastrada em " + sector.getCode());
        return ProductionLineResponse.from(saved);
    }

    @Transactional
    public ProductionLineResponse updateLine(UUID id, UpdateProductionLineRequest request) {
        ProductionLine line = findLine(id);
        Sector sector = findSector(request.sectorId());
        String code = normalizeCode(request.code());
        if (productionLineRepository.existsBySectorIdAndCodeIgnoreCaseAndIdNot(sector.getId(), code, id)) {
            throw new ConflictException("Já existe uma linha com este código neste setor.");
        }
        line.update(sector, request.name().trim(), code);
        auditService.record("LINE_UPDATED", "PRODUCTION_LINE", line.getId(), "Linha " + line.getCode() + " atualizada");
        return ProductionLineResponse.from(line);
    }

    @Transactional
    public ProductionLineResponse updateLineStatus(UUID id, UpdateStructureStatusRequest request) {
        ProductionLine line = findLine(id);
        if (request.active()) validateSectorHierarchy(line.getSector());
        line.changeActive(request.active());
        auditService.record("LINE_STATUS_CHANGED", "PRODUCTION_LINE", line.getId(), "Linha " + line.getCode() + (request.active() ? " ativada" : " desativada"));
        return ProductionLineResponse.from(line);
    }

    private Plant findPlant(UUID id) {
        return plantRepository.findById(id).orElseThrow(() -> new NotFoundException("Planta não encontrada."));
    }

    private Sector findSector(UUID id) {
        return sectorRepository.findById(id).orElseThrow(() -> new NotFoundException("Setor não encontrado."));
    }

    private ProductionLine findLine(UUID id) {
        return productionLineRepository.findById(id).orElseThrow(() -> new NotFoundException("Linha de produção não encontrada."));
    }

    private void validateSectorHierarchy(Sector sector) {
        if (!sector.isActive()) throw new ConflictException("Ative o setor antes de cadastrar ou ativar linhas nele.");
        if (!sector.getPlant().isActive()) throw new ConflictException("Ative a planta antes de cadastrar ou ativar linhas nela.");
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
