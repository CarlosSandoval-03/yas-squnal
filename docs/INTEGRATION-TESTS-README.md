# Tests de Integración - Guía de Ejecución

## Estructura Implementada

Tests de integración para los microservicios Cart, Payment y Order siguiendo arquitectura de 3 niveles:

### Nivel 1: Backend ↔ Database
- Validan persistencia real en PostgreSQL usando Testcontainers
- Ubicación: `src/it/java/**/*IT.java`

### Nivel 2: Frontend ↔ Backend
- Validan contrato API y estructura JSON de respuestas HTTP
- Validan headers HTTP y códigos de estado
- Usan MockMvc para simular requests HTTP
- Usan jwt() para autenticación en tests
- Ubicación: `src/it/java/**/*Level2IT.java`

### Nivel 3: End-to-End
- Tests E2E con Selenium WebDriver y llamadas HTTP reales
- Validan flujo completo: API → Database → Frontend
- Agregan datos vía API real, verifican en DB, y validan que el frontend los muestra
- No usan mocks para las llamadas HTTP al backend
- Ubicación: `src/it/java/**/*E2EIT.java`

---

## Módulos con Tests de Integración

### Cart Service

**Nivel 1 - Backend ↔ Database:**
- Archivo: `cart/src/it/java/com/yas/cart/controller/CartItemControllerIT.java`
- Casos: CART-BE-INT-01 a CART-BE-INT-04 (agregar, leer, actualizar, eliminar items)

**Nivel 2 - Frontend ↔ Backend:**
- Archivo: `cart/src/it/java/com/yas/cart/controller/CartItemControllerLevel2IT.java`
- Casos: CART-FE-INT-01, CART-FE-INT-02 (validación de estructura JSON y contrato API)

**Nivel 3 - End-to-End:**
- Archivo: `cart/src/it/java/com/yas/cart/controller/CartItemControllerE2EIT.java`
- Casos: CART-E2E-01 (flujo completo: agregar, ver, actualizar, eliminar items)

### Payment Service

**Nivel 1 - Backend ↔ Database:**
- Archivo: `payment/src/it/java/com/yas/payment/controller/PaymentControllerIT.java`
- Casos: PAYMENT-BE-INT-01, PAYMENT-BE-INT-02 (captura exitosa y fallida)

**Nivel 2 - Frontend ↔ Backend:**
- Archivo: `payment/src/it/java/com/yas/payment/controller/PaymentControllerLevel2IT.java`
- Casos: PAYMENT-FE-INT-01, PAYMENT-FE-INT-02, PAYMENT-FE-INT-03 (init, capture, cancel)

### Order Service

**Nivel 1 - Backend ↔ Database:**
- Archivo: `order/src/it/java/com/yas/order/controller/CheckoutControllerIT.java`
- Casos: ORDER-BE-INT-01, ORDER-BE-INT-02 (crear checkout, actualizar estado)

**Nivel 2 - Frontend ↔ Backend:**
- Archivo: `order/src/it/java/com/yas/order/controller/CheckoutControllerLevel2IT.java`
- Casos: ORDER-FE-INT-01, ORDER-FE-INT-02 (crear checkout, obtener checkout)

---

## Ejecutar Tests de Integración

### Scripts Automatizados

**Ejecutar tests de Nivel 1 (Backend ↔ Database):**

Windows:
```cmd
run-integration-tests-level1.bat
```

Linux/Mac/Git Bash:
```bash
./run-integration-tests-level1.sh
```

**Ejecutar tests de Nivel 2 (Frontend ↔ Backend):**

Windows:
```cmd
run-integration-tests-level2.bat
```

Linux/Mac/Git Bash:
```bash
./run-integration-tests-level2.sh
```

**Ejecutar tests de Nivel 3 (End-to-End):**

IMPORTANTE: Los tests E2E requieren que el frontend esté corriendo.

1. Iniciar el frontend (desde otra terminal):
```bash
cd storefront
npm install
npm run dev
```

2. Iniciar el backend Cart service (si no está corriendo):
```bash
cd cart
mvn spring-boot:run
```

3. Ejecutar los tests E2E:

Windows (PowerShell):
```powershell
cd cart
mvn clean test-compile failsafe:integration-test "-Dsurefire.skip=true" "-Dit.test=CartItemControllerE2EIT" "-Dfailsafe.failIfNoSpecifiedTests=false" "-Dfrontend.url=http://localhost:3000"
```

Windows (CMD):
```cmd
cd cart
mvn clean test-compile failsafe:integration-test -Dsurefire.skip=true -Dit.test=CartItemControllerE2EIT -Dfailsafe.failIfNoSpecifiedTests=false -Dfrontend.url=http://localhost:3000
```

Linux/Mac/Git Bash:
```bash
cd cart
mvn clean test-compile failsafe:integration-test -Dsurefire.skip=true -Dit.test=CartItemControllerE2EIT -Dfailsafe.failIfNoSpecifiedTests=false -Dfrontend.url=http://localhost:3000
```

Linux/Mac/Git Bash:
```bash
cd cart
mvn clean compile failsafe:integration-test -Dsurefire.skip=true -Dit.test="*E2EIT" -Dfrontend.url=http://localhost:3000
```

Nota: Si el frontend está en otro puerto, ajusta `-Dfrontend.url` según corresponda.

Los scripts ejecutan los tests en orden y muestran un resumen al final.

### Ejecutar Tests Manualmente

### Prerequisitos
- Docker Desktop corriendo (obligatorio para Testcontainers)
- Maven 3.8+
- Java 21
- Proyecto compilado (common-library instalado)
- Chrome/Chromium instalado (para tests E2E con Selenium)
- Frontend corriendo en http://localhost:3000 (para tests E2E)
- Node.js y npm instalados (para ejecutar el frontend)

### Paso 1: Verificar Docker Desktop

Docker Desktop debe estar corriendo antes de ejecutar los tests.

```powershell
docker ps
```

Si Docker no está corriendo, inicia Docker Desktop y espera a que esté completamente iniciado.

### Paso 2: Compilar el proyecto (primera vez)

```powershell
mvn clean install -DskipTests -pl common-library
mvn clean install -DskipTests -pl cart
```

### Paso 3: Ejecutar tests de integración

**Ejecutar tests de Nivel 1 (Backend ↔ Database):**

```powershell
cd cart
mvn clean compile failsafe:integration-test -Dsurefire.skip=true -Dit.test="*IT" -Dit.test="!*Level2IT"

cd payment
mvn clean compile failsafe:integration-test -Dsurefire.skip=true -Dit.test="*IT" -Dit.test="!*Level2IT"

cd order
mvn clean compile failsafe:integration-test -Dsurefire.skip=true -Dit.test="*IT" -Dit.test="!*Level2IT"
```

**Ejecutar tests de Nivel 2 (Frontend ↔ Backend):**

```powershell
cd cart
mvn clean compile failsafe:integration-test -Dsurefire.skip=true -Dit.test="*Level2IT"

cd payment
mvn clean compile failsafe:integration-test -Dsurefire.skip=true -Dit.test="*Level2IT"

cd order
mvn clean compile failsafe:integration-test -Dsurefire.skip=true -Dit.test="*Level2IT"
```

**Ejecutar un test específico:**

```powershell
# Test específico de Nivel 1
cd cart
mvn failsafe:integration-test -Dit.test=CartItemControllerIT#testAddCartItem_ThenReadFromDatabase

# Test específico de Nivel 2 (validación de contrato API)
cd cart
mvn failsafe:integration-test -Dit.test=CartItemControllerLevel2IT#testGetCartItems_ValidatesJsonStructureForFrontend

# Test específico de Nivel 3 (E2E)
cd cart
mvn failsafe:integration-test -Dit.test=CartItemControllerE2EIT#testCompleteCartFlow_E2E
```

### Paso 4: Ver resultados

Los tests mostrarán `BUILD SUCCESS` si pasan, o el error específico si fallan.

Ejemplo de salida exitosa:
```
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Solución de Problemas Comunes

#### Error: "Could not find artifact com.yas:common-library"
```powershell
# Solución: Compilar common-library primero
cd "C:\Users\gagar\Desktop\Diplomado QA\yas-squnal"
mvn clean install -DskipTests -pl common-library
```

#### Error: "Docker daemon not running"
```powershell
# Solución: Iniciar Docker Desktop
# 1. Abre Docker Desktop desde el menú de inicio
# 2. Espera a que inicie completamente
# 3. Verifica con: docker ps
```

#### Error: "Testcontainers cannot connect to Docker"
```powershell
# Solución: Verificar configuración de Docker
docker info
# Si funciona, Docker está bien configurado
```

#### Los tests fallan con errores de conexión
- Verifica que Docker Desktop está corriendo
- Verifica que tienes suficiente memoria RAM (Testcontainers necesita recursos)
- Cierra otras aplicaciones que usen Docker

---

## Configuración

### Testcontainers

Testcontainers se usa para levantar una base de datos PostgreSQL real en un contenedor Docker durante la ejecución de los tests.

**Ventajas:**
- Base de datos real: Los tests validan persistencia real, no mocks
- Aislamiento: Cada ejecución de tests usa su propia base de datos limpia
- Automatización: El contenedor se crea antes de los tests y se destruye después
- Consistencia: Mismo entorno de base de datos en todos los ambientes (desarrollo, CI/CD)

**Configuración:**
- PostgreSQL 16 en contenedor Docker
- Se levanta automáticamente antes de los tests
- Se destruye automáticamente después de los tests
- Configuración en `@DynamicPropertySource` para inyectar URL, usuario y contraseña

### Configuración de Tests

Cada módulo tiene su archivo `src/it/resources/application-it.properties` con configuración específica para tests de integración.

---

## Características de los Tests

### Nivel 1 (Backend ↔ Database)
- Base de datos real PostgreSQL usando Testcontainers
- Tests transaccionales que limpian datos después de cada ejecución
- Validan persistencia real en base de datos
- Usan `MockedStatic` para mockear `AuthenticationUtils` (no hay Keycloak en tests)

### Nivel 2 (Frontend ↔ Backend)
- Validan estructura JSON de respuestas HTTP
- Validan headers HTTP y códigos de estado
- Validan el contrato API que el frontend espera consumir
- Usan `MockMvc` para simular requests HTTP sin levantar servidor completo
- Usan `jwt()` para autenticación en tests

### Nivel 3 (End-to-End)
- Validan flujo completo: API HTTP real → Database → Frontend
- Agregan items al carrito usando llamadas HTTP reales (RestTemplate)
- Verifican persistencia en base de datos real (Testcontainers)
- Validan que el frontend muestra los datos agregados (Selenium)
- Actualizan y eliminan items vía API, verifican en DB y frontend
- No usan mocks para las llamadas HTTP (a diferencia de Nivel 1 y 2)
- Limpian datos automáticamente al final para garantizar consistencia
- Usan Chrome con interfaz gráfica (configurable para headless)
- Requieren que el frontend esté corriendo para ejecutarse

## Próximos Pasos

- Tests Nivel 3 (E2E) con Playwright (pendiente)

