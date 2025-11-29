# Diseño de Tests de Integración - YAS Project

## Objetivo
Implementar tests de integración para los microservicios críticos: **Cart**, **Payment**, y **Order**, validando el flujo completo desde Frontend → Backend → Database.

## Arquitectura de Tests de Integración

| Nivel | Tipo de Test | Valida | Tecnología |
|-------|--------------|--------|------------|
| **Nivel 1** | Backend ↔ Database | Ejecución SQL vía HTTP requests | `@SpringBootTest` + `Testcontainers` (PostgreSQL) |
| **Nivel 2** | Frontend ↔ Backend | Uso de API desde frontend con backend real | `@SpringBootTest` + `MockMvc` / `RestAssured` |
| **Nivel 3** | End-to-End | Simulación de navegador, stack completo, base de datos real | `Playwright` / `Selenium` |

Cada nivel aumenta progresivamente el acoplamiento y disminuye el uso de mocks.

---

## Casos de Prueba por Microservicio

### 1. CART Service

#### Nivel 1: Backend ↔ Database

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|-------------------|
| **CART-BE-INT-01** | Agregar item al carrito | POST `/storefront/cart/items` con `productId` y `quantity` | Item visible en DB vía GET `/storefront/cart/items` |
| **CART-BE-INT-02** | Leer items del carrito | GET `/storefront/cart/items` | JSON contiene items previamente insertados |
| **CART-BE-INT-03** | Actualizar cantidad de item | PUT `/storefront/cart/items/{productId}` con nueva cantidad | Cantidad actualizada en DB |
| **CART-BE-INT-04** | Eliminar item del carrito | POST `/storefront/cart/items/remove` con `productId` | Item eliminado de DB |

#### Nivel 2: Frontend ↔ Backend

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|-------------------|
| **CART-FE-INT-01** | Frontend obtiene items del carrito | Cargar componente React que llama a `/storefront/cart/items` | UI muestra lista de items que coinciden con respuesta API |
| **CART-FE-INT-02** | Frontend agrega item al carrito | Usuario completa formulario y envía POST | Item aparece en UI después de respuesta exitosa |

#### Nivel 3: End-to-End

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|-------------------|
| **CART-E2E-01** | Flujo completo agregar item | Usuario navega → Selecciona producto → Agrega al carrito → Verifica en carrito | Item visible en página de carrito |
| **CART-E2E-02** | Flujo completo actualizar cantidad | Usuario en carrito → Modifica cantidad → Guarda | Cantidad actualizada visible en UI |

---

### 2. PAYMENT Service

#### Nivel 1: Backend ↔ Database

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|-------------------|
| **PAYMENT-BE-INT-01** | Captura exitosa de pago | POST `/capture` con `paymentMethod=PAYPAL` y token válido | Respuesta HTTP 200 y registro persistido con estado `COMPLETED` |
| **PAYMENT-BE-INT-02** | Captura fallida de pago | POST `/capture` con token inválido simulado | Registro persistido con estado `FAILED` y `failureMessage` |

#### Nivel 2: Frontend ↔ Backend

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|-------------------|
| **PAYMENT-FE-INT-01** | Frontend inicializa pago | Componente React llama a `/payment/init` | UI muestra información de pago iniciado |
| **PAYMENT-FE-INT-02** | Frontend captura pago | Usuario confirma pago → POST `/payment/capture` | UI muestra confirmación de pago exitoso |

#### Nivel 3: End-to-End

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|-------------------|
| **PAYMENT-E2E-01** | Flujo completo de pago | Usuario en checkout → Selecciona método de pago → Confirma → Pago procesado | Pago completado y orden creada |
| **PAYMENT-E2E-02** | Cancelar pago | Usuario inicia pago → Cancela | Estado de pago actualizado a `CANCELLED` |

---

### 3. ORDER Service (Checkout)

#### Nivel 1: Backend ↔ Database

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|-------------------|
| **ORDER-BE-INT-01** | Crear checkout | POST `/storefront/checkouts` con items y método de pago | Checkout + items persistidos con estado `PENDING` |
| **ORDER-BE-INT-02** | Actualizar estado a COMPLETED | PUT `/storefront/checkouts/status` con `checkoutId` | Estado del checkout actualizado y se devuelve `orderId` |

#### Nivel 2: Frontend ↔ Backend

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|-------------------|
| **ORDER-FE-INT-01** | Frontend crea checkout | Componente React llama a `/storefront/checkouts` | UI muestra checkout creado con items |
| **ORDER-FE-INT-02** | Frontend obtiene checkout | Componente carga checkout por ID | UI muestra detalles del checkout |

#### Nivel 3: End-to-End

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|-------------------|
| **ORDER-E2E-01** | Flujo completo de checkout | Usuario en carrito → Procede a checkout → Completa información → Confirma | Orden creada y visible en historial |
| **ORDER-E2E-02** | Flujo checkout con pago | Checkout → Selección método pago → Pago → Confirmación | Orden completada con pago procesado |

---

## Flujos de Integración Entre Servicios

### Flujo Completo: Cart → Order → Payment

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|-------------------|
| **FLOW-INT-01** | Flujo completo de compra | 1. Agregar items a cart<br>2. Crear checkout desde cart<br>3. Inicializar pago<br>4. Capturar pago<br>5. Completar orden | Datos consistentes en Cart, Order y Payment DBs |

---

## Estructura de Archivos

```
cart/
  src/
    it/                          # Integration tests
      java/
        com/yas/cart/
          controller/
            CartItemControllerIT.java           # Nivel 1: Backend ↔ DB
            CartItemControllerLevel2IT.java     # Nivel 2: Frontend ↔ Backend
      resources/
        application-it.properties              # Configuración para tests

payment/
  src/
    it/
      java/
        com/yas/payment/
          controller/
            PaymentControllerIT.java           # Nivel 1: Backend ↔ DB
            PaymentControllerLevel2IT.java     # Nivel 2: Frontend ↔ Backend
      resources/
        application-it.properties

order/
  src/
    it/
      java/
        com/yas/order/
          controller/
            CheckoutControllerIT.java          # Nivel 1: Backend ↔ DB
            CheckoutControllerLevel2IT.java    # Nivel 2: Frontend ↔ Backend
      resources/
        application-it.properties
```

---

## Tecnologías y Herramientas

### Backend Tests (Nivel 1 y 2)
- **@SpringBootTest**: Levanta contexto completo de Spring
- **Testcontainers**: PostgreSQL real en contenedor Docker
- **MockMvc**: Para simular requests HTTP en tests de Nivel 2
- **JUnit 5**: Framework de testing

### E2E Tests (Nivel 3)
- **Playwright**: Para automatización de navegador (pendiente)
- **Docker Compose**: Para levantar stack completo (pendiente)

---

## Testcontainers

Testcontainers levanta una base de datos PostgreSQL real en un contenedor Docker durante los tests.

**Ventajas:**
- Base de datos real: Los tests validan persistencia real, no mocks
- Aislamiento: Cada ejecución usa su propia base de datos limpia
- Automatización: El contenedor se crea antes de los tests y se destruye después
- Consistencia: Mismo entorno en todos los ambientes (desarrollo, CI/CD)

**Configuración:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CartItemControllerIT {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("cart_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

## Criterios de Éxito

- Tests de integración cubren flujos críticos de negocio
- Tests validan persistencia real en base de datos usando Testcontainers
- Tests validan contrato API y estructura JSON para frontend
- Tests son ejecutables de forma independiente
- Tests usan datos aislados (no interfieren entre sí)

---

## Estado de Implementación

- Estructura de carpetas `src/it` creada
- Dependencias de Testcontainers agregadas
- Tests Nivel 1 (Backend ↔ DB) implementados para Cart, Payment y Order
- Tests Nivel 2 (Frontend ↔ Backend) implementados para Cart, Payment y Order
- Scripts de ejecución automatizados creados
- Documentación actualizada
- Tests Nivel 3 (E2E) pendiente

