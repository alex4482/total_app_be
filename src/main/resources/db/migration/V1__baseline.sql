-- ============================================================================
-- BASELINE MIGRATION - Complete database schema
-- This migration creates all tables needed for the application
-- ============================================================================

-- ============================================================================
-- SEQUENCES
-- ============================================================================

CREATE SEQUENCE IF NOT EXISTS backup_metadata_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS location_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS tenant_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS index_counter_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS index_data_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS tenant_rental_data_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS service_formula_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS service_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS service_monthly_values_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS email_preset_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- ============================================================================
-- ENUMS (PostgreSQL doesn't have native enums, using VARCHAR with CHECK constraints)
-- ============================================================================

-- Note: Enums are stored as VARCHAR in PostgreSQL
-- CounterType: APA, GAZ, CURENT_220V, CURENT_380V
-- LocationType: BUILDING, ROOM
-- BuildingLocation: STANGA, DREAPTA, CENTRU, etc.
-- Currency: RON, EUR, USD
-- BackupType: MANUAL, AUTOMATIC
-- ObservationUrgency: SIMPLE, URGENT, TODO

-- ============================================================================
-- MAIN TABLES
-- ============================================================================

-- Location (abstract base class for Building and Room)
CREATE TABLE IF NOT EXISTS location (
    id BIGINT NOT NULL PRIMARY KEY,
    dtype VARCHAR(31) NOT NULL, -- Discriminator: Building, Room, RentalSpace
    name VARCHAR(255) NOT NULL UNIQUE,
    official_name VARCHAR(255),
    location SMALLINT, -- BuildingLocation enum (ORDINAL - implicit)
    mp INTEGER,
    type SMALLINT -- LocationType enum (ORDINAL - implicit)
);

-- Building extends Location (JOINED inheritance)
CREATE TABLE IF NOT EXISTS building (
    id BIGINT NOT NULL PRIMARY KEY,
    CONSTRAINT fk_building_location FOREIGN KEY (id) REFERENCES location(id) ON DELETE CASCADE
);

-- Room extends Location (JOINED inheritance)
CREATE TABLE IF NOT EXISTS room (
    id BIGINT NOT NULL PRIMARY KEY,
    building_id BIGINT,
    ground_level BOOLEAN,
    CONSTRAINT fk_room_location FOREIGN KEY (id) REFERENCES location(id) ON DELETE CASCADE,
    CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE
);

-- RentalSpace extends Room (JOINED inheritance)
-- @PrimaryKeyJoinColumn(name = "name") means the join column is named "name" (not "id")
-- This column references room.id (the parent's primary key)
CREATE TABLE IF NOT EXISTS rental_space (
    name BIGINT NOT NULL PRIMARY KEY,
    rental_agreement_id BIGINT,
    CONSTRAINT fk_rental_space_room FOREIGN KEY (name) REFERENCES room(id) ON DELETE CASCADE
);

-- Tenant
CREATE TABLE IF NOT EXISTS tenant (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    cui VARCHAR(50),
    reg_number VARCHAR(50),
    pf BOOLEAN,
    active BOOLEAN
);

-- IndexCounter
CREATE TABLE IF NOT EXISTS index_counter (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255),
    location_id BIGINT,
    counter_type VARCHAR(50),
    location_type VARCHAR(50),
    building_location VARCHAR(50),
    default_unit_price DOUBLE PRECISION,
    CONSTRAINT fk_counter_location FOREIGN KEY (location_id) REFERENCES location(id) ON DELETE CASCADE
);

-- IndexData (base class for reading data)
CREATE TABLE IF NOT EXISTS index_data (
    id BIGINT NOT NULL PRIMARY KEY,
    dtype VARCHAR(31), -- Discriminator: IndexData, ReplacedCounterIndexData
    index DOUBLE PRECISION,
    consumption DOUBLE PRECISION,
    type VARCHAR(50),
    reading_date DATE,
    unit_price DOUBLE PRECISION,
    total_cost DOUBLE PRECISION,
    counter_id BIGINT NOT NULL,
    CONSTRAINT fk_index_data_counter FOREIGN KEY (counter_id) REFERENCES index_counter(id) ON DELETE CASCADE
);

-- ReplacedCounterIndexData extends IndexData (JOINED inheritance)
CREATE TABLE IF NOT EXISTS replaced_counter_index_data (
    id BIGINT NOT NULL PRIMARY KEY,
    old_index_data_id BIGINT NOT NULL UNIQUE,
    new_counter_initial_index DOUBLE PRECISION,
    replacement_date DATE,
    CONSTRAINT fk_replaced_index_data FOREIGN KEY (id) REFERENCES index_data(id) ON DELETE CASCADE,
    CONSTRAINT fk_replaced_old_data FOREIGN KEY (old_index_data_id) REFERENCES index_data(id) ON DELETE CASCADE
);

-- TenantRentalData
CREATE TABLE IF NOT EXISTS tenant_rental_data (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rental_space_name BIGINT, -- OneToOne with RentalSpace (references rental_space.name which is the primary key)
    start_date DATE,
    end_date DATE,
    rent DOUBLE PRECISION,
    currency VARCHAR(50) DEFAULT 'RON',
    contract_number VARCHAR(255),
    contract_date DATE,
    CONSTRAINT fk_rental_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_space FOREIGN KEY (rental_space_name) REFERENCES rental_space(name) ON DELETE CASCADE
);

-- Add foreign key constraint from rental_space to tenant_rental_data (after both tables exist)
ALTER TABLE rental_space 
    ADD CONSTRAINT fk_rental_space_agreement 
    FOREIGN KEY (rental_agreement_id) REFERENCES tenant_rental_data(id) ON DELETE SET NULL;

-- ServiceFormula (must be created before Service)
CREATE TABLE IF NOT EXISTS service_formula (
    id BIGINT NOT NULL PRIMARY KEY,
    expression VARCHAR(500) NOT NULL,
    description TEXT
);

-- Service
CREATE TABLE IF NOT EXISTS service (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    unit_of_measure VARCHAR(50),
    default_monthly_cost DOUBLE PRECISION,
    formula_id BIGINT,
    default_include_in_report BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_service_formula FOREIGN KEY (formula_id) REFERENCES service_formula(id) ON DELETE SET NULL
);

-- ServiceMonthlyValue
CREATE TABLE IF NOT EXISTS service_monthly_values (
    id BIGINT NOT NULL PRIMARY KEY,
    rental_data_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    custom_value DOUBLE PRECISION,
    is_manual BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(1000),
    updated_at TIMESTAMP,
    CONSTRAINT fk_monthly_value_rental FOREIGN KEY (rental_data_id) REFERENCES tenant_rental_data(id) ON DELETE CASCADE,
    CONSTRAINT uq_monthly_value UNIQUE (rental_data_id, service_id, year, month)
);

-- FileAsset
CREATE TABLE IF NOT EXISTS file_asset (
    id UUID NOT NULL PRIMARY KEY,
    owner_type VARCHAR(32) NOT NULL,
    owner_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    size_bytes BIGINT,
    checksum VARCHAR(64),
    modified_at TIMESTAMP,
    uploaded_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_owner_name UNIQUE (owner_type, owner_id, original_filename),
    CONSTRAINT uq_owner_checksum UNIQUE (owner_type, owner_id, checksum)
);

-- TempUpload
CREATE TABLE IF NOT EXISTS temp_upload (
    id UUID NOT NULL PRIMARY KEY,
    batch_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    size_bytes BIGINT,
    checksum VARCHAR(64),
    temp_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

-- EmailPreset
CREATE TABLE IF NOT EXISTS email_preset (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255),
    subject VARCHAR(255),
    message TEXT,
    recipients TEXT[], -- PostgreSQL array
    keywords TEXT[] -- PostgreSQL array
);

-- Reminder
CREATE TABLE IF NOT EXISTS reminder (
    id UUID NOT NULL PRIMARY KEY,
    email_title VARCHAR(500) NOT NULL,
    email_message TEXT NOT NULL,
    expiration_date TIMESTAMP NOT NULL,
    warning_start_date TIMESTAMP NOT NULL,
    warning_email_count INTEGER NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    emails_sent_count INTEGER NOT NULL DEFAULT 0,
    last_email_sent_at TIMESTAMP,
    expired_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- RefreshTokenState
CREATE TABLE IF NOT EXISTS refresh_token_state (
    session_id VARCHAR(36) NOT NULL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    previous_token_hash VARCHAR(64),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    revoked_after TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    ip VARCHAR(45),
    user_agent VARCHAR(200),
    CONSTRAINT idx_rts_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_rts_prev_hash ON refresh_token_state(previous_token_hash);

-- BackupMetadata
CREATE TABLE IF NOT EXISTS backup_metadata (
    id BIGINT NOT NULL PRIMARY KEY,
    backup_name VARCHAR(255) NOT NULL UNIQUE,
    backup_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    local_path VARCHAR(500) NOT NULL,
    google_drive_file_id VARCHAR(255),
    size_bytes BIGINT,
    description TEXT
);

-- ============================================================================
-- ELEMENT COLLECTION TABLES
-- ============================================================================

-- Location observations
CREATE TABLE IF NOT EXISTS location_observations (
    location_id BIGINT NOT NULL,
    observations_order INTEGER,
    message TEXT,
    type VARCHAR(50),
    PRIMARY KEY (location_id, observations_order),
    CONSTRAINT fk_location_obs_location FOREIGN KEY (location_id) REFERENCES location(id) ON DELETE CASCADE
);

-- Location counter prices
CREATE TABLE IF NOT EXISTS location_counter_prices (
    location_id BIGINT NOT NULL,
    counter_type VARCHAR(50) NOT NULL,
    unit_price DOUBLE PRECISION,
    PRIMARY KEY (location_id, counter_type),
    CONSTRAINT fk_location_counter_prices_location FOREIGN KEY (location_id) REFERENCES location(id) ON DELETE CASCADE
);

-- Tenant emails
CREATE TABLE IF NOT EXISTS tenant_emails (
    tenant_id BIGINT NOT NULL,
    emails_order INTEGER,
    emails VARCHAR(255),
    PRIMARY KEY (tenant_id, emails_order),
    CONSTRAINT fk_tenant_emails_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- Tenant phone numbers
CREATE TABLE IF NOT EXISTS tenant_phone_numbers (
    tenant_id BIGINT NOT NULL,
    phone_numbers_order INTEGER,
    phone_numbers VARCHAR(50),
    PRIMARY KEY (tenant_id, phone_numbers_order),
    CONSTRAINT fk_tenant_phones_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- Tenant observations
CREATE TABLE IF NOT EXISTS tenant_observations (
    tenant_id BIGINT NOT NULL,
    observations_order INTEGER,
    message TEXT,
    type VARCHAR(50),
    PRIMARY KEY (tenant_id, observations_order),
    CONSTRAINT fk_tenant_obs_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- Tenant attachment IDs
CREATE TABLE IF NOT EXISTS tenant_attachment_ids (
    tenant_id BIGINT NOT NULL,
    attachment_ids_order INTEGER,
    attachment_ids VARCHAR(255),
    PRIMARY KEY (tenant_id, attachment_ids_order),
    CONSTRAINT fk_tenant_attachments_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- TenantRentalData price changes
CREATE TABLE IF NOT EXISTS tenant_rental_data_price_changes (
    tenant_rental_data_id BIGINT NOT NULL,
    price_changes_order INTEGER,
    new_price DOUBLE PRECISION,
    change_time DATE,
    PRIMARY KEY (tenant_rental_data_id, price_changes_order),
    CONSTRAINT fk_price_changes_rental FOREIGN KEY (tenant_rental_data_id) REFERENCES tenant_rental_data(id) ON DELETE CASCADE
);

-- TenantRentalData active services
CREATE TABLE IF NOT EXISTS tenant_rental_data_active_services (
    tenant_rental_data_id BIGINT NOT NULL,
    active_services_order INTEGER,
    service_id BIGINT NOT NULL,
    custom_monthly_cost DOUBLE PRECISION,
    include_in_report BOOLEAN,
    active_from DATE,
    active_until DATE,
    notes VARCHAR(1000),
    PRIMARY KEY (tenant_rental_data_id, active_services_order),
    CONSTRAINT fk_active_services_rental FOREIGN KEY (tenant_rental_data_id) REFERENCES tenant_rental_data(id) ON DELETE CASCADE
);

-- Reminder groupings
CREATE TABLE IF NOT EXISTS reminder_grouping (
    reminder_id UUID NOT NULL,
    grouping_name VARCHAR(255),
    CONSTRAINT fk_reminder_grouping FOREIGN KEY (reminder_id) REFERENCES reminder(id) ON DELETE CASCADE
);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE location IS 'Base table for Building, Room, and RentalSpace (JOINED inheritance)';
COMMENT ON TABLE tenant IS 'Tenant information';
COMMENT ON TABLE index_counter IS 'Counter definitions (water, gas, electricity)';
COMMENT ON TABLE index_data IS 'Reading data for counters (JOINED inheritance with ReplacedCounterIndexData)';
COMMENT ON TABLE tenant_rental_data IS 'Rental agreements between tenants and rental spaces';
COMMENT ON TABLE service IS 'Service definitions (salubrizare, alarma, etc.)';
COMMENT ON TABLE service_formula IS 'Formulas for calculating service values';
COMMENT ON TABLE service_monthly_values IS 'Custom monthly values for services';
COMMENT ON TABLE file_asset IS 'File metadata stored in database';
COMMENT ON TABLE temp_upload IS 'Temporary file uploads before commit';
COMMENT ON TABLE email_preset IS 'Email template presets';
COMMENT ON TABLE reminder IS 'Reminders for expiration dates';
COMMENT ON TABLE refresh_token_state IS 'JWT refresh token state management';
COMMENT ON TABLE backup_metadata IS 'Backup metadata information';

