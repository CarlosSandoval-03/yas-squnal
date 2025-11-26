# Instrucciones del Laboratorio 2 - Pruebas de Integración

## Objetivos de Aprendizaje

Al finalizar este laboratorio, serás capaz de:

1. ✅ Entender las pruebas de integración y su rol después de las pruebas unitarias
2. ✅ Diseñar pruebas de integración entre componentes base de datos ↔ backend ↔ frontend
3. ✅ Implementar pruebas de integración usando herramientas y fixtures apropiadas
4. ✅ Ejecutar pruebas de integración localmente y dentro de Docker
5. ✅ Comparar resultados entre integraciones exitosas y rotas

## Sistema Bajo Prueba

El sistema implementado tiene tres componentes:

| Componente | Tecnología | Propósito |
|------------|-----------|-----------|
| sq-fe | Frontend (React/JS) | Muestra tareas |
| sq-be | Backend (Python/Flask) | Almacena y retorna tareas |
| sq-db | Base de datos (MySQL) | Persiste tareas |

## Alcance de las Pruebas de Integración

A diferencia de las pruebas unitarias (cada componente probado independientemente), las pruebas de integración validan:

- ✅ Comunicación correcta entre sq-be ↔ sq-db
- ✅ Comunicación correcta entre sq-fe ↔ sq-be
- ✅ Flujo de datos end-to-end a través de sq-fe ↔ sq-be ↔ sq-db

## Arquitectura de Pruebas de Integración

| Nivel | Tipo de Prueba | Valida |
|-------|----------------|--------|
| Nivel 1 | Backend ↔ Database | Ejecución SQL vía requests HTTP |
| Nivel 2 | Frontend ↔ Backend | Uso de API desde frontend con backend real |
| Nivel 3 | End-to-End | Simulación de navegador, stack completo, base de datos real |

## Casos de Prueba Implementados

### Nivel 1 — Backend ↔ Database

**Archivo:** `sq-be/tests_integration/test_be_db_integration.py`

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|---------------------|
| BE-INT-01 | Insertar tarea | POST /tasks con "Task INT" | Fila visible en DB vía GET |
| BE-INT-02 | Leer tareas | GET /tasks | JSON contiene tarea previamente insertada |

### Nivel 2 — Frontend ↔ Backend

**Archivo:** `sq-fe/src/__tests__/integration.test.js`

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|---------------------|
| FE-INT-01 | Frontend obtiene tareas | Cargar app React | UI muestra items de lista que coinciden con respuesta /tasks |

### Nivel 3 — End-to-End (Recomendado)

**Archivo:** `sq-e2e/tests/e2e.spec.js`

| ID | Escenario | Pasos | Resultado Esperado |
|----|-----------|-------|---------------------|
| E2E-01 | Flujo completo | POST vía UI → GET vía UI | Tarea aparece después de acción del usuario |

## Ejecución Manual de Pruebas

### Paso 1: Levantar servicios base

En una terminal:

```bash
docker compose -f docker-compose.sq.yml up sq-db sq-be
```

Espera a que ambos servicios estén saludables (verás mensajes de healthcheck).

### Paso 2: Pruebas de integración Backend

En otra terminal:

```bash
cd sq-be
pytest tests_integration -v
```

**Resultado esperado:** Todas las pruebas pasan (✓)

### Paso 3: Pruebas de integración Frontend

En otra terminal (mientras sq-be sigue corriendo):

```bash
cd sq-fe
npm install  # Solo la primera vez
npm test -- src/__tests__/integration.test.js --watchAll=false
```

**Resultado esperado:** Todas las pruebas pasan (✓)

### Paso 4 (Opcional): Pruebas End-to-End

En otra terminal (con sq-fe corriendo en puerto 3000):

```bash
cd sq-e2e
npm install  # Solo la primera vez
npx playwright install  # Instalar navegadores
npx playwright test
```

## Ejecución Automatizada con Docker

Para ejecutar todas las pruebas de integración automáticamente:

```bash
docker compose -f docker-compose.sq.yml up --build sq-be-int-tests sq-fe-int-tests
```

Este comando:
1. Construye las imágenes necesarias
2. Levanta sq-db y sq-be
3. Espera a que estén saludables
4. Ejecuta las pruebas de integración del backend
5. Ejecuta las pruebas de integración del frontend
6. Muestra los resultados

## Ejecutar Sistema Completo

Para levantar todos los servicios (incluyendo frontend):

```bash
docker compose -f docker-compose.sq.yml up --build
```

Luego accede a:
- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:5000
- **Health Check:** http://localhost:5000/health
- **API Tasks:** http://localhost:5000/tasks

## Verificación de Resultados

### Pruebas Exitosas

Cuando todas las pruebas pasan, deberías ver:

```
✅ Backend ↔ Database: Todas las pruebas pasan
✅ Frontend ↔ Backend: Todas las pruebas pasan
✅ End-to-End: Todas las pruebas pasan
```

### Pruebas Fallidas

Si alguna prueba falla, verás mensajes de error específicos:

```
❌ Backend ↔ Database: Falla si no puede conectar a DB
❌ Frontend ↔ Backend: Falla si no puede conectar al backend
❌ End-to-End: Falla si el flujo completo no funciona
```

## Comparar Integraciones Exitosas vs Rotas

### Experimento 1: Romper la conexión Backend-Database

1. Detén el servicio sq-db: `docker compose -f docker-compose.sq.yml stop sq-db`
2. Ejecuta las pruebas: `cd sq-be && pytest tests_integration -v`
3. **Resultado esperado:** Las pruebas fallan con error de conexión

### Experimento 2: Romper la conexión Frontend-Backend

1. Detén el servicio sq-be: `docker compose -f docker-compose.sq.yml stop sq-be`
2. Ejecuta las pruebas del frontend: `cd sq-fe && npm test -- src/__tests__/integration.test.js --watchAll=false`
3. **Resultado esperado:** Las pruebas fallan con error de red

### Experimento 3: Verificar flujo completo

1. Levanta todos los servicios: `docker compose -f docker-compose.sq.yml up`
2. Abre http://localhost:3000
3. Agrega una tarea desde la UI
4. Verifica que aparece en la lista
5. Verifica en la base de datos: `docker exec -it <sq-db-container> mysql -uroot -ppassword tasks_db -e "SELECT * FROM tasks;"`

## Estructura de Archivos

```
sq/
├── sq-be/                          # Backend Python
│   ├── app.py                      # Aplicación Flask
│   ├── requirements.txt
│   ├── Dockerfile
│   └── tests_integration/
│       └── test_be_db_integration.py  # Pruebas Nivel 1
├── sq-fe/                          # Frontend React
│   ├── src/
│   │   ├── App.js
│   │   └── __tests__/
│   │       └── integration.test.js    # Pruebas Nivel 2
│   ├── package.json
│   └── Dockerfile
├── sq-e2e/                         # Pruebas E2E
│   ├── tests/
│   │   └── e2e.spec.js                # Pruebas Nivel 3
│   └── package.json
└── docker-compose.sq.yml           # Configuración Docker
```

## Notas Importantes

⚠️ **Sin mocks, sin stubs**: Las pruebas de integración usan comunicación real entre componentes

⚠️ **Base de datos real**: Se usa MySQL real, no in-memory

⚠️ **Backend real**: El frontend se conecta al backend real, no a mocks

⚠️ **E2E real**: Playwright simula un navegador real

## Solución de Problemas

### Error: "Connection refused" en pruebas backend
- Verifica que sq-be y sq-db estén corriendo
- Verifica que los healthchecks pasen

### Error: "Network error" en pruebas frontend
- Verifica que sq-be esté accesible en http://localhost:5000
- Verifica la variable de entorno REACT_APP_API_BASE

### Error: "Database connection failed"
- Verifica que sq-db esté corriendo
- Verifica las credenciales en docker-compose.sq.yml

## Entregables

Cada equipo debe completar:

1. ✅ Sistema con dos o más componentes interconectados (sq-fe, sq-be, sq-db)
2. ✅ Identificación de puntos de integración y relaciones arquitectónicas
3. ✅ Diseño de al menos 5 casos de prueba de integración
4. ✅ Implementación de pruebas de integración usando comunicación real entre componentes
5. ✅ Sin mocks, sin stubs, sin reemplazos in-memory

## Referencias

- [Flask Documentation](https://flask.palletsprojects.com/)
- [React Testing Library](https://testing-library.com/react)
- [Playwright Documentation](https://playwright.dev/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

