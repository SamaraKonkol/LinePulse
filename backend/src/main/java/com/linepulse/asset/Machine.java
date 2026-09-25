package com.linepulse.asset;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "machines")
public class Machine {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_line_id")
    private ProductionLine productionLine;

    private String name;
    private String assetCode;
    private String manufacturer;
    private String model;
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    private MachineStatus status;

    private LocalDate installedAt;
    private Instant createdAt;
    private Instant updatedAt;

    protected Machine() {
    }

    public Machine(UUID id, ProductionLine productionLine, String name, String assetCode, String manufacturer, String model, String serialNumber, MachineStatus status, LocalDate installedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.productionLine = productionLine;
        this.name = name;
        this.assetCode = assetCode;
        this.manufacturer = manufacturer;
        this.model = model;
        this.serialNumber = serialNumber;
        this.status = status;
        this.installedAt = installedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void changeStatus(MachineStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public String getName() {
        return name;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public LocalDate getInstalledAt() {
        return installedAt;
    }
}
