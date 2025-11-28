# Tests de Integración - Guía de Ejecución

## Estructura Implementada

Se han creado tests de integración para los microservicios críticos siguiendo la arquitectura de 3 niveles:

### Nivel 1: Backend ↔ Database
- Tests que validan la persistencia real en base de datos PostgreSQL usando Testcontainers
- Ubicación: `src/it/java/**/*IT.java`

### Nivel 2: Frontend ↔ Backend
- Tests que validan la comunicación entre frontend y backend (pendiente de implementar)

### Nivel 3: End-to-End
- Tests E2E con Playwright (pendiente de implementar)

---

## Módulos con Tests de Integración

### ✅ Cart Service
- **Archivo**: `cart/src/it/java/com/yas/cart/controller/CartItemControllerIT.java`
- **Casos implementados**:
  - CART-BE-INT-01: Agregar item al carrito y verificar persistencia
  - CART-BE-INT-02: Leer items del carrito y verificar estructura JSON
  - CART-BE-INT-03: Actualizar cantidad de item
  - CART-BE-INT-04: Eliminar item del carrito

### ✅ Payment Service
- **Archivo**: `payment/src/it/java/com/yas/payment/controller/PaymentControllerIT.java`
- **Casos implementados**:
  - PAYMENT-BE-INT-01: Captura exitosa → persistencia en tabla `payment`
  - PAYMENT-BE-INT-02: Captura fallida → registro con `failureMessage`

### ✅ Order Service
- **Archivo**: `order/src/it/java/com/yas/order/controller/CheckoutControllerIT.java`
- **Casos implementados**:
  - ORDER-BE-INT-01: Creación de checkout con items enriquecidos
  - ORDER-BE-INT-02: Actualización de estado `COMPLETED` y retorno de `orderId`

---

## Ejecutar Tests de Integración

### Scripts Automatizados

Para ejecutar **todos los tests de Nivel 1** (Cart, Payment, Order) de una vez:

**Windows (Command Prompt o PowerShell):**
```cmd
cd "C:\Users\gagar\Desktop\Diplomado QA\yas-squnal"
run-integration-tests-level1.bat
```

**Linux/Mac/Git Bash:**
```bash
cd yas-squnal
chmod +x run-integration-tests-level1.sh
./run-integration-tests-level1.sh
```

Los scripts ejecutan los tests en orden y muestran un resumen al final indicando qué módulos pasaron o fallaron.

### Ejecutar Tests Manualmente

### Prerequisitos
- ✅ **Docker Desktop corriendo** (para Testcontainers - **OBLIGATORIO**)
- ✅ Maven 3.8+
- ✅ Java 21
- ✅ Proyecto compilado (common-library instalado)

### Paso 1: Verificar Docker Desktop

**IMPORTANTE**: Docker Desktop debe estar corriendo antes de ejecutar los tests.

```powershell
# Verificar que Docker está corriendo
docker ps
# Si funciona, verás una lista (puede estar vacía, está bien)
```

Si Docker no está corriendo:
1. Abre Docker Desktop
2. Espera a que esté completamente iniciado (ícono de Docker en la bandeja del sistema)
3. Verifica con `docker ps` nuevamente

### Paso 2: Compilar el proyecto (primera vez)

```powershell
# Desde la raíz del proyecto
cd "C:\Users\gagar\Desktop\Diplomado QA\yas-squnal"

# Compilar common-library primero (requerido por cart)
mvn clean install -DskipTests -pl common-library

# Luego compilar cart
mvn clean install -DskipTests -pl cart
```

### Paso 3: Ejecutar tests de integración de Cart

```powershell
# Opción 1: Ejecutar solo tests de integración
cd cart
mvn failsafe:integration-test

# Opción 2: Ejecutar todos los tests (unitarios + integración)
cd cart
mvn verify

# Opción 3: Ejecutar un test específico
cd cart
mvn test -Dtest=CartItemControllerIT#testAddCartItem_ThenReadFromDatabase
```

### Paso 4: Ver resultados

Los tests mostrarán:
- ✅ **Tests pasando**: Verás `BUILD SUCCESS`
- ❌ **Tests fallando**: Verás el error específico y stack trace

**Ejemplo de salida exitosa:**
```
[INFO] --- failsafe:integration-test (default) @ cart ---
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.yas.cart.controller.CartItemControllerIT
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 15.234 s
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
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
Los tests usan PostgreSQL 16 en contenedor Docker. El contenedor se levanta automáticamente y se destruye después de los tests.

### Configuración de Tests
Cada módulo tiene su archivo `src/it/resources/application-it.properties` con configuración específica para tests de integración.

---

## Próximos Pasos

1. ✅ Estructura base creada
2. ✅ Tests de integración para Cart (Nivel 1)
3. ✅ Tests de integración para Payment (Nivel 1)
4. ✅ Tests de integración para Order (Nivel 1)
5. ⏳ Tests Nivel 2 (Frontend ↔ Backend)
6. ⏳ Tests Nivel 3 (E2E)

---

## Notas Importantes

- Los tests de integración usan una base de datos real (PostgreSQL en Docker)
- Los tests son transaccionales y limpian datos después de cada ejecución
- Se usa `MockedStatic` para mockear `AuthenticationUtils` ya que no hay Keycloak en tests
- Los tests validan tanto la respuesta HTTP como la persistencia en base de datos

