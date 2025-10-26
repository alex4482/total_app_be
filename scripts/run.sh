#!/bin/bash
# Script pentru rulare rapidă Total App (Linux/Mac/Git Bash)
# Rulează cu: ./run.sh

echo "🚀 Starting Total App..."
echo ""

# Verifică dacă Java 21 este instalat
echo "Checking Java version..."
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n 1)
    echo "✅ $java_version"
else
    echo "❌ Java not found! Please install JDK 21"
    exit 1
fi

echo ""

# Verifică dacă PostgreSQL rulează
echo "Checking PostgreSQL..."
if command -v pg_isready &> /dev/null; then
    if pg_isready -h localhost -p 5432 &> /dev/null; then
        echo "✅ PostgreSQL is running"
    else
        echo "⚠️  PostgreSQL is not responding on localhost:5432"
    fi
else
    echo "⚠️  pg_isready not found (PostgreSQL might still be running)"
fi

echo ""
echo "Starting Spring Boot application..."
echo "Press Ctrl+C to stop"
echo ""

# Rulează aplicația
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

