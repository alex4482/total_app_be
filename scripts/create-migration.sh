#!/bin/bash
# Script pentru crearea unei noi migrări Flyway
# Usage: ./create-migration.sh "add_phone_to_tenant"

set -e

if [ -z "$1" ]; then
    echo "❌ Trebuie să specifici o descriere pentru migrare!"
    echo "   Usage: $0 \"description_here\""
    echo "   Example: $0 \"add_phone_to_tenant\""
    exit 1
fi

DESCRIPTION="$1"
MIGRATIONS_DIR="src/main/resources/db/migration"

# Verifică că directorul există
if [ ! -d "$MIGRATIONS_DIR" ]; then
    echo "❌ Directorul $MIGRATIONS_DIR nu există!"
    echo "   Rulează mai întâi: mkdir -p $MIGRATIONS_DIR"
    exit 1
fi

# Găsește ultimul număr de versiune
LAST_VERSION=0
for file in "$MIGRATIONS_DIR"/V*.sql; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        if [[ $filename =~ ^V([0-9]+)__ ]]; then
            version="${BASH_REMATCH[1]}"
            if [ "$version" -gt "$LAST_VERSION" ]; then
                LAST_VERSION="$version"
            fi
        fi
    fi
done

NEXT_VERSION=$((LAST_VERSION + 1))

# Normalizează descrierea
NORMALIZED_DESC=$(echo "$DESCRIPTION" | sed 's/[^a-zA-Z0-9_]/_/g' | sed 's/__*/_/g')

# Numele fișierului
FILENAME="V${NEXT_VERSION}__${NORMALIZED_DESC}.sql"
FILEPATH="$MIGRATIONS_DIR/$FILENAME"

# Template-ul migrării
cat > "$FILEPATH" << EOF
-- Migration: $DESCRIPTION
-- Version: V$NEXT_VERSION
-- Date: $(date '+%Y-%m-%d %H:%M')
-- Author: $(whoami)
--
-- Description:
-- Add your description here
--

-- Your SQL commands here:

-- Example: Add column
-- ALTER TABLE table_name 
-- ADD COLUMN column_name data_type;

-- Example: Create index
-- CREATE INDEX idx_table_column ON table_name(column_name);

-- Example: Add constraint
-- ALTER TABLE table_name 
-- ADD CONSTRAINT constraint_name 
-- FOREIGN KEY (column_name) REFERENCES other_table(id);

-- TODO: Replace examples with actual SQL commands
EOF

echo ""
echo "✅ Migrare creată cu succes!"
echo ""
echo "📁 Fișier: $FILENAME"
echo "📍 Path: $FILEPATH"
echo "🔢 Versiune: V$NEXT_VERSION"
echo ""
echo "📝 Următorii pași:"
echo "   1. Deschide fișierul și adaugă SQL-ul tău"
echo "   2. Testează migrarea pe o copie a bazei de date"
echo "   3. Commit și push pe main"
echo ""

# Oferă să deschidă fișierul
read -p "Vrei să deschizi fișierul acum? (y/n) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command -v code &> /dev/null; then
        code "$FILEPATH"
        echo "✅ Fișier deschis în VS Code"
    elif command -v nano &> /dev/null; then
        nano "$FILEPATH"
    elif command -v vi &> /dev/null; then
        vi "$FILEPATH"
    else
        echo "⚠️  Nu am găsit un editor. Deschide manual: $FILEPATH"
    fi
fi

echo ""
echo "📚 Pentru mai multe informații:"
echo "   - docs/MIGRATION_SETUP.md"
echo "   - guides/07-database-migrations.md"
echo ""

