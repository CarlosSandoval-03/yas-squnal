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

Los scripts ejecutan todos los tests de integración por nivel para los módulos Cart, Payment y Order.

**Nivel 1 (Backend ↔ Database):**

Windows:
```cmd
run-integration-tests-level1.bat
```

Linux/Mac/Git Bash:
```bash
./run-integration-tests-level1.sh
```

**Nivel 2 (Frontend ↔ Backend):**

Windows:
```cmd
run-integration-tests-level2.bat
```

Linux/Mac/Git Bash:
```bash
./run-integration-tests-level2.sh
```

**Nivel 3 (End-to-End):**

Requisitos:
- Docker Desktop corriendo
- Frontend corriendo en http://localhost:3000
- Backend Cart service corriendo
- Chrome/Chromium instalado

Pasos:

1. Iniciar el frontend:
```bash
cd storefront
npm install
npm run dev
```

2. Iniciar el backend Cart service:
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

### Prerequisitos

- Docker Desktop corriendo (obligatorio para Testcontainers)
- Maven 3.8+
- Java 21
- Proyecto compilado (common-library instalado)

Para tests E2E adicionales:
- Chrome/Chromium instalado
- Frontend corriendo en http://localhost:3000
- Node.js y npm instalados

### Solución de Problemas

**Error: "Could not find artifact com.yas:common-library"**
```powershell
mvn clean install -DskipTests -pl common-library
```

**Error: "Docker daemon not running"**
Iniciar Docker Desktop y verificar con `docker ps`.

**Error: "Testcontainers cannot connect to Docker"**
Verificar configuración con `docker info`.

---

## Configuración

**Testcontainers:**
- PostgreSQL 16 en contenedor Docker
- Se levanta automáticamente antes de los tests y se destruye después
- Configuración en `@DynamicPropertySource` para inyectar URL, usuario y contraseña

**Configuración de Tests:**
Cada módulo tiene su archivo `src/it/resources/application-it.properties` con configuración específica para tests de integración.

---

## Características de los Tests

**Nivel 1 (Backend ↔ Database):**
- Base de datos real PostgreSQL usando Testcontainers
- Tests transaccionales que limpian datos después de cada ejecución
- Validan persistencia real en base de datos
- Usan `MockedStatic` para mockear `AuthenticationUtils`

**Nivel 2 (Frontend ↔ Backend):**
- Validan estructura JSON de respuestas HTTP
- Validan headers HTTP y códigos de estado
- Validan el contrato API que el frontend espera consumir
- Usan `MockMvc` para simular requests HTTP
- Usan `jwt()` para autenticación en tests

**Nivel 3 (End-to-End):**
- Validan flujo completo: API HTTP real → Database → Frontend
- Usan llamadas HTTP reales (RestTemplate) y Selenium WebDriver
- Verifican persistencia en base de datos real (Testcontainers)
- Validan que el frontend muestra los datos agregados
- No usan mocks para las llamadas HTTP
- Requieren que el frontend esté corriendo

