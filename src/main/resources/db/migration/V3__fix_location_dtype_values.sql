-- Migration: Fix NULL dtype values in location table
-- Version: V3
-- Date: 2025-12-02
--
-- Description:
-- Sets the dtype discriminator column for existing location records
-- that may have NULL values. This is required for JPA inheritance
-- to work correctly with the JOINED strategy.
--
-- IMPORTANT: This migration is safe to run even if tables are empty.
-- The UPDATE statements will simply affect 0 rows if no data exists.
--
-- Order matters:
-- 1. RentalSpace (most specific - also exists in room table)
-- 2. Building
-- 3. Room (after RentalSpace to avoid conflicts)

-- Fix RentalSpace records (must be first since RentalSpace extends Room)
-- Safe: Will update 0 rows if rental_space table is empty
UPDATE location
SET dtype = 'RentalSpace'
WHERE dtype IS NULL
  AND id IN (SELECT name FROM rental_space WHERE name IS NOT NULL);

-- Fix Building records
-- Safe: Will update 0 rows if building table is empty
UPDATE location
SET dtype = 'Building'
WHERE dtype IS NULL
  AND id IN (SELECT id FROM building WHERE id IS NOT NULL);

-- Fix Room records (that are not RentalSpace)
-- Safe: Will update 0 rows if room table is empty
UPDATE location
SET dtype = 'Room'
WHERE dtype IS NULL
  AND id IN (SELECT id FROM room WHERE id IS NOT NULL)
  AND id NOT IN (SELECT name FROM rental_space WHERE name IS NOT NULL);

-- Verify no NULL values remain (should return 0 rows)
-- SELECT COUNT(*) FROM location WHERE dtype IS NULL;

