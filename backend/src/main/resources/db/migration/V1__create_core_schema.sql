CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE plants (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(40) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE sectors (
    id UUID PRIMARY KEY,
    plant_id UUID NOT NULL REFERENCES plants(id),
    name VARCHAR(120) NOT NULL,
    code VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sector_plant_code UNIQUE (plant_id, code)
);

CREATE TABLE production_lines (
    id UUID PRIMARY KEY,
    sector_id UUID NOT NULL REFERENCES sectors(id),
    name VARCHAR(120) NOT NULL,
    code VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_line_sector_code UNIQUE (sector_id, code)
);

CREATE TABLE machines (
    id UUID PRIMARY KEY,
    production_line_id UUID NOT NULL REFERENCES production_lines(id),
    name VARCHAR(140) NOT NULL,
    asset_code VARCHAR(60) NOT NULL UNIQUE,
    manufacturer VARCHAR(120),
    model VARCHAR(120),
    serial_number VARCHAR(120),
    status VARCHAR(30) NOT NULL,
    installed_at DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
