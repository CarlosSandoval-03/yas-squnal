@echo off
REM Script para ejecutar todos los tests de integración Nivel 1 (Backend ↔ Database)
REM Microservicios: Cart, Payment, Order

echo ========================================
echo Tests de Integracion - Nivel 1
echo Backend ^<^> Database
echo ========================================
echo.

set PROJECT_ROOT=%~dp0
cd /d "%PROJECT_ROOT%"

echo [1/3] Ejecutando tests de Cart...
echo ----------------------------------------
call mvn -pl cart -am clean compile failsafe:integration-test -Dsurefire.skip=true -Drevision=1.0-SNAPSHOT
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Tests de Cart fallaron
    set CART_FAILED=1
) else (
    echo.
    echo [OK] Tests de Cart pasaron correctamente
    set CART_FAILED=0
)
echo.

echo [2/3] Ejecutando tests de Payment...
echo ----------------------------------------
call mvn -pl payment -am clean compile failsafe:integration-test -Dsurefire.skip=true -Drevision=1.0-SNAPSHOT
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Tests de Payment fallaron
    set PAYMENT_FAILED=1
) else (
    echo.
    echo [OK] Tests de Payment pasaron correctamente
    set PAYMENT_FAILED=0
)
echo.

echo [3/3] Ejecutando tests de Order...
echo ----------------------------------------
call mvn -pl order -am clean compile failsafe:integration-test -Dsurefire.skip=true -Drevision=1.0-SNAPSHOT
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Tests de Order fallaron
    set ORDER_FAILED=1
) else (
    echo.
    echo [OK] Tests de Order pasaron correctamente
    set ORDER_FAILED=0
)
echo.

echo ========================================
echo Resumen de Resultados
echo ========================================
echo.

if "%CART_FAILED%"=="1" (
    echo [X] Cart: FALLIDO
) else (
    echo [OK] Cart: EXITOSO
)

if "%PAYMENT_FAILED%"=="1" (
    echo [X] Payment: FALLIDO
) else (
    echo [OK] Payment: EXITOSO
)

if "%ORDER_FAILED%"=="1" (
    echo [X] Order: FALLIDO
) else (
    echo [OK] Order: EXITOSO
)

echo.

if "%CART_FAILED%"=="1" (
    exit /b 1
)
if "%PAYMENT_FAILED%"=="1" (
    exit /b 1
)
if "%ORDER_FAILED%"=="1" (
    exit /b 1
)

echo ========================================
echo Todos los tests pasaron correctamente!
echo ========================================
exit /b 0

