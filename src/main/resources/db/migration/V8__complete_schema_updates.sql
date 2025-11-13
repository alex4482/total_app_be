-- ============================================================================
-- Complete schema updates: location prices, services, and collections
-- This migration combines multiple schema changes needed for the application
-- ============================================================================

-- ============================================================================
-- PART 1: Location Counter Prices
-- ============================================================================
-- Create location_counter_prices table for storing default unit prices per counter type per location
-- This table is used by @ElementCollection in Location entity to store List<CounterTypePrice>

CREATE TABLE IF NOT EXISTS location_counter_prices (
    location_id BIGINT NOT NULL,
    counter_type VARCHAR(50) NOT NULL,
    unit_price DOUBLE PRECISION,
    PRIMARY KEY (location_id, counter_type),
    CONSTRAINT fk_location_counter_prices_location 
        FOREIGN KEY (location_id) REFERENCES location(id) ON DELETE CASCADE
);

-- Create index for better query performance
CREATE INDEX idx_location_counter_prices_location_id ON location_counter_prices(location_id);
CREATE INDEX idx_location_counter_prices_counter_type ON location_counter_prices(counter_type);

-- Add comments
COMMENT ON TABLE location_counter_prices IS 'Stores default unit prices per counter type for each location';
COMMENT ON COLUMN location_counter_prices.location_id IS 'Foreign key to location table';
COMMENT ON COLUMN location_counter_prices.counter_type IS 'Type of counter (WATER, GAS, ELECTRICITY_220, ELECTRICITY_380)';
COMMENT ON COLUMN location_counter_prices.unit_price IS 'Default unit price for this counter type at this location';

-- ============================================================================
-- PART 2: Service Tables
-- ============================================================================
-- Create service and service_formula tables
-- Service: General service definition (reusable across rental agreements)
-- ServiceFormula: Formula for calculating service values dynamically

-- Create service_formula table first (referenced by service)
CREATE TABLE IF NOT EXISTS service_formula (
    id BIGSERIAL PRIMARY KEY,
    expression VARCHAR(500) NOT NULL,
    description TEXT
);

COMMENT ON TABLE service_formula IS 'Stores formulas for calculating service values dynamically';
COMMENT ON COLUMN service_formula.expression IS 'Formula expression (e.g., "rent * 0.03", "waterConsumption * 0.5 + 20")';
COMMENT ON COLUMN service_formula.description IS 'Description of what the formula calculates';

-- Create service table
CREATE TABLE IF NOT EXISTS service (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    unit_of_measure VARCHAR(50),
    default_monthly_cost DOUBLE PRECISION,
    formula_id BIGINT,
    default_include_in_report BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_service_formula FOREIGN KEY (formula_id) 
        REFERENCES service_formula(id) ON DELETE CASCADE
);

-- Create index on service name for faster lookups
CREATE INDEX idx_service_name ON service(name);
CREATE INDEX idx_service_active ON service(active);

COMMENT ON TABLE service IS 'General service definitions (reusable across rental agreements)';
COMMENT ON COLUMN service.name IS 'Service name (e.g., "Salubrizare", "Alarma", "Întreținere")';
COMMENT ON COLUMN service.description IS 'Optional description';
COMMENT ON COLUMN service.unit_of_measure IS 'Unit of measure (e.g., "lei", "mc", "kw")';
COMMENT ON COLUMN service.default_monthly_cost IS 'Default monthly cost (can be overridden per rental agreement)';
COMMENT ON COLUMN service.formula_id IS 'Foreign key to service_formula (if formula-based calculation)';
COMMENT ON COLUMN service.default_include_in_report IS 'Default flag for including this service in consumption reports';
COMMENT ON COLUMN service.active IS 'Active/inactive flag (soft delete)';

-- Create service_monthly_values table for custom service values per month
CREATE TABLE IF NOT EXISTS service_monthly_values (
    id BIGSERIAL PRIMARY KEY,
    rental_data_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    custom_value DOUBLE PRECISION,
    is_manual BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(1000),
    updated_at TIMESTAMP,
    CONSTRAINT uk_service_monthly_values UNIQUE (rental_data_id, service_id, year, month),
    CONSTRAINT fk_service_monthly_values_rental_data FOREIGN KEY (rental_data_id) 
        REFERENCES tenant_rental_data(id) ON DELETE CASCADE,
    CONSTRAINT fk_service_monthly_values_service FOREIGN KEY (service_id) 
        REFERENCES service(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_service_monthly_values_rental_data ON service_monthly_values(rental_data_id);
CREATE INDEX idx_service_monthly_values_service ON service_monthly_values(service_id);
CREATE INDEX idx_service_monthly_values_year_month ON service_monthly_values(year, month);

COMMENT ON TABLE service_monthly_values IS 'Custom service values for specific months (overrides calculated values)';
COMMENT ON COLUMN service_monthly_values.rental_data_id IS 'Foreign key to tenant_rental_data';
COMMENT ON COLUMN service_monthly_values.service_id IS 'Foreign key to service';
COMMENT ON COLUMN service_monthly_values.year IS 'Year for this value';
COMMENT ON COLUMN service_monthly_values.month IS 'Month (0-11, where 0=January, 11=December)';
COMMENT ON COLUMN service_monthly_values.custom_value IS 'Custom value (overrides calculated value)';
COMMENT ON COLUMN service_monthly_values.is_manual IS 'Whether this value was set manually (true) or is calculated (false)';

-- ============================================================================
-- PART 3: Tenant Rental Data Collections
-- ============================================================================
-- Create collection tables for TenantRentalData @ElementCollection fields
-- These tables store embedded collections (ActiveService and PriceData)

-- Table for active_services collection
-- Stores active services for each rental agreement with their configuration
CREATE TABLE IF NOT EXISTS tenant_rental_data_active_services (
    tenant_rental_data_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    custom_monthly_cost DOUBLE PRECISION,
    include_in_report BOOLEAN,
    active_from DATE,
    active_until DATE,
    notes VARCHAR(1000),
    PRIMARY KEY (tenant_rental_data_id, service_id),
    CONSTRAINT fk_active_services_rental_data FOREIGN KEY (tenant_rental_data_id) 
        REFERENCES tenant_rental_data(id) ON DELETE CASCADE
);

-- Create index for better query performance
CREATE INDEX idx_active_services_rental_data ON tenant_rental_data_active_services(tenant_rental_data_id);
CREATE INDEX idx_active_services_service_id ON tenant_rental_data_active_services(service_id);

COMMENT ON TABLE tenant_rental_data_active_services IS 'Stores active services for rental agreements with custom configuration';
COMMENT ON COLUMN tenant_rental_data_active_services.tenant_rental_data_id IS 'Foreign key to tenant_rental_data';
COMMENT ON COLUMN tenant_rental_data_active_services.service_id IS 'Service ID (reference to service table)';
COMMENT ON COLUMN tenant_rental_data_active_services.custom_monthly_cost IS 'Custom monthly cost (overrides service default)';
COMMENT ON COLUMN tenant_rental_data_active_services.include_in_report IS 'Whether to include in reports (null = use service default)';
COMMENT ON COLUMN tenant_rental_data_active_services.active_from IS 'Date from which service is active';
COMMENT ON COLUMN tenant_rental_data_active_services.active_until IS 'Date until which service is active (null = indefinitely)';

-- Table for price_changes collection
-- Stores price change history for rental agreements
CREATE TABLE IF NOT EXISTS tenant_rental_data_price_changes (
    tenant_rental_data_id BIGINT NOT NULL,
    new_price DOUBLE PRECISION NOT NULL,
    change_time DATE NOT NULL,
    PRIMARY KEY (tenant_rental_data_id, new_price, change_time),
    CONSTRAINT fk_price_changes_rental_data FOREIGN KEY (tenant_rental_data_id) 
        REFERENCES tenant_rental_data(id) ON DELETE CASCADE
);

CREATE INDEX idx_price_changes_rental_data ON tenant_rental_data_price_changes(tenant_rental_data_id);
CREATE INDEX idx_price_changes_change_time ON tenant_rental_data_price_changes(change_time);

COMMENT ON TABLE tenant_rental_data_price_changes IS 'Stores price change history for rental agreements';
COMMENT ON COLUMN tenant_rental_data_price_changes.tenant_rental_data_id IS 'Foreign key to tenant_rental_data';
COMMENT ON COLUMN tenant_rental_data_price_changes.new_price IS 'New price value';
COMMENT ON COLUMN tenant_rental_data_price_changes.change_time IS 'Date when price changed';

-- ============================================================================
-- PART 4: Element Collection Tables
-- ============================================================================
-- Create collection tables for @ElementCollection fields that don't have explicit @CollectionTable
-- These tables store embedded collections for Tenant and Location entities

-- Table for tenant emails collection
CREATE TABLE IF NOT EXISTS tenant_emails (
    tenant_id BIGINT NOT NULL,
    emails VARCHAR(255),
    PRIMARY KEY (tenant_id, emails),
    CONSTRAINT fk_tenant_emails_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_tenant_emails_tenant ON tenant_emails(tenant_id);

COMMENT ON TABLE tenant_emails IS 'Stores email addresses for tenants';

-- Table for tenant phone numbers collection
CREATE TABLE IF NOT EXISTS tenant_phone_numbers (
    tenant_id BIGINT NOT NULL,
    phone_numbers VARCHAR(50),
    PRIMARY KEY (tenant_id, phone_numbers),
    CONSTRAINT fk_tenant_phone_numbers_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_tenant_phone_numbers_tenant ON tenant_phone_numbers(tenant_id);

COMMENT ON TABLE tenant_phone_numbers IS 'Stores phone numbers for tenants';

-- Table for tenant observations collection (Observation is Embeddable with message and type)
CREATE TABLE IF NOT EXISTS tenant_observations (
    tenant_id BIGINT NOT NULL,
    observations_message VARCHAR(1000),
    observations_type VARCHAR(50),
    PRIMARY KEY (tenant_id, observations_message, observations_type),
    CONSTRAINT fk_tenant_observations_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_tenant_observations_tenant ON tenant_observations(tenant_id);

COMMENT ON TABLE tenant_observations IS 'Stores observations for tenants (message and urgency type)';

-- Table for tenant attachment IDs collection
CREATE TABLE IF NOT EXISTS tenant_attachment_ids (
    tenant_id BIGINT NOT NULL,
    attachment_ids VARCHAR(255),
    PRIMARY KEY (tenant_id, attachment_ids),
    CONSTRAINT fk_tenant_attachment_ids_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_tenant_attachment_ids_tenant ON tenant_attachment_ids(tenant_id);

COMMENT ON TABLE tenant_attachment_ids IS 'Stores attachment IDs for tenants';

-- Table for location observations collection (Observation is Embeddable with message and type)
CREATE TABLE IF NOT EXISTS location_observations (
    location_id BIGINT NOT NULL,
    observations_message VARCHAR(1000),
    observations_type VARCHAR(50),
    PRIMARY KEY (location_id, observations_message, observations_type),
    CONSTRAINT fk_location_observations_location FOREIGN KEY (location_id) 
        REFERENCES location(id) ON DELETE CASCADE
);

CREATE INDEX idx_location_observations_location ON location_observations(location_id);

COMMENT ON TABLE location_observations IS 'Stores observations for locations (message and urgency type)';

