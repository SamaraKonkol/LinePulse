package com.linepulse.asset;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plants")
public class Plant {
    @Id
    private UUID id;
    private String name;
    private String code;
    private boolean active;
    private Instant createdAt;

    protected Plant() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}
