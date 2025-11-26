#!/bin/bash

# Script para ejecutar pruebas de integración

echo "=== Ejecutando Pruebas de Integración SQ ==="
echo ""

# Opción 1: Ejecutar con Docker Compose (recomendado)
if [ "$1" == "docker" ]; then
    echo "Ejecutando pruebas con Docker Compose..."
    docker compose -f docker-compose.sq.yml up --build sq-be-int-tests sq-fe-int-tests
    exit $?
fi

# Opción 2: Ejecutar manualmente (requiere servicios corriendo)
echo "Asegúrate de que los servicios estén corriendo:"
echo "  docker compose -f docker-compose.sq.yml up sq-db sq-be"
echo ""
echo "Ejecutando pruebas de integración del backend..."
cd sq-be
pytest tests_integration -v
cd ..

echo ""
echo "Ejecutando pruebas de integración del frontend..."
cd sq-fe
npm test -- src/__tests__/integration.test.js --watchAll=false
cd ..

echo ""
echo "=== Pruebas completadas ==="

