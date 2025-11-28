#!/bin/bash
# Script para ejecutar todos los tests de integración Nivel 1 (Backend ↔ Database)
# Microservicios: Cart, Payment, Order

echo "========================================"
echo "Tests de Integración - Nivel 1"
echo "Backend ↔ Database"
echo "========================================"
echo ""

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

CART_FAILED=0
PAYMENT_FAILED=0
ORDER_FAILED=0

echo "[1/3] Ejecutando tests de Cart..."
echo "----------------------------------------"
mvn -pl cart -am clean compile failsafe:integration-test -Dsurefire.skip=true -Drevision=1.0-SNAPSHOT 2>&1 | grep -v "No tests to run"
if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Tests de Cart fallaron"
    CART_FAILED=1
else
    echo ""
    echo "[OK] Tests de Cart pasaron correctamente"
fi
echo ""

echo "[2/3] Ejecutando tests de Payment..."
echo "----------------------------------------"
mvn -pl payment -am clean compile failsafe:integration-test -Dsurefire.skip=true -Drevision=1.0-SNAPSHOT 2>&1 | grep -v "No tests to run"
if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Tests de Payment fallaron"
    PAYMENT_FAILED=1
else
    echo ""
    echo "[OK] Tests de Payment pasaron correctamente"
fi
echo ""

echo "[3/3] Ejecutando tests de Order..."
echo "----------------------------------------"
mvn -pl order -am clean compile failsafe:integration-test -Dsurefire.skip=true -Drevision=1.0-SNAPSHOT 2>&1 | grep -v "No tests to run"
if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Tests de Order fallaron"
    ORDER_FAILED=1
else
    echo ""
    echo "[OK] Tests de Order pasaron correctamente"
fi
echo ""

echo "========================================"
echo "Resumen de Resultados"
echo "========================================"
echo ""

if [ $CART_FAILED -eq 1 ]; then
    echo "[X] Cart: FALLIDO"
else
    echo "[OK] Cart: EXITOSO"
fi

if [ $PAYMENT_FAILED -eq 1 ]; then
    echo "[X] Payment: FALLIDO"
else
    echo "[OK] Payment: EXITOSO"
fi

if [ $ORDER_FAILED -eq 1 ]; then
    echo "[X] Order: FALLIDO"
else
    echo "[OK] Order: EXITOSO"
fi

echo ""

if [ $CART_FAILED -eq 1 ] || [ $PAYMENT_FAILED -eq 1 ] || [ $ORDER_FAILED -eq 1 ]; then
    echo "Algunos tests fallaron. Revisar logs."
    exit 1
fi

echo "========================================"
echo "Todos los tests pasan :) "
echo "========================================"
exit 0

