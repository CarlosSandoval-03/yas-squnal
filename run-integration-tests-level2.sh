#!/bin/bash
# Script para ejecutar todos los tests de integración Nivel 2 (Frontend ↔ Backend)
# Microservicios: Cart, Payment, Order

echo "========================================"
echo "Tests de Integración - Nivel 2"
echo "Frontend ↔ Backend"
echo "========================================"
echo ""

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

CART_FAILED=0
PAYMENT_FAILED=0
ORDER_FAILED=0

echo "[1/3] Ejecutando tests de Cart (Nivel 2)..."
echo "----------------------------------------"
cd "$PROJECT_ROOT/cart"
mvn clean compile failsafe:integration-test -Dsurefire.skip=true -Dit.test="*Level2IT" -Dfailsafe.failIfNoSpecifiedTests=false 2>&1 | grep -v "No tests to run"
MAVEN_EXIT_CODE=${PIPESTATUS[0]}
if [ $MAVEN_EXIT_CODE -ne 0 ]; then
    echo ""
    echo "[ERROR] Tests de Cart fallaron"
    CART_FAILED=1
else
    echo ""
    echo "[OK] Tests de Cart pasaron correctamente"
fi
cd "$PROJECT_ROOT"
echo ""

echo "[2/3] Ejecutando tests de Payment (Nivel 2)..."
echo "----------------------------------------"
cd "$PROJECT_ROOT/payment"
mvn clean compile failsafe:integration-test -Dsurefire.skip=true -Dit.test="*Level2IT" -Dfailsafe.failIfNoSpecifiedTests=false 2>&1 | grep -v "No tests to run"
MAVEN_EXIT_CODE=${PIPESTATUS[0]}
if [ $MAVEN_EXIT_CODE -ne 0 ]; then
    echo ""
    echo "[ERROR] Tests de Payment fallaron"
    PAYMENT_FAILED=1
else
    echo ""
    echo "[OK] Tests de Payment pasaron correctamente"
fi
cd "$PROJECT_ROOT"
echo ""

echo "[3/3] Ejecutando tests de Order (Nivel 2)..."
echo "----------------------------------------"
cd "$PROJECT_ROOT/order"
mvn clean compile failsafe:integration-test -Dsurefire.skip=true -Dit.test="*Level2IT" -Dfailsafe.failIfNoSpecifiedTests=false 2>&1 | grep -v "No tests to run"
MAVEN_EXIT_CODE=${PIPESTATUS[0]}
if [ $MAVEN_EXIT_CODE -ne 0 ]; then
    echo ""
    echo "[ERROR] Tests de Order fallaron"
    ORDER_FAILED=1
else
    echo ""
    echo "[OK] Tests de Order pasaron correctamente"
fi
cd "$PROJECT_ROOT"
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
    echo "Algunos tests fallaron. Revisa los logs arriba."
    exit 1
fi

echo "========================================"
echo "Todos los tests pasan :)"
echo "========================================"
exit 0


