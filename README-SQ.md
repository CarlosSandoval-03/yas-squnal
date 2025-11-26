# SQ - Integration Testing Laboratory

Este proyecto implementa un sistema de gestión de tareas (tasks) con pruebas de integración en tres niveles.

## Estructura del Proyecto

```
sq/
├── sq-be/              # Backend Python (Flask)
│   ├── app.py         # Aplicación Flask principal
│   ├── requirements.txt
│   ├── Dockerfile
│   └── tests_integration/
│       └── test_be_db_integration.py
├── sq-fe/              # Frontend React
│   ├── src/
│   │   ├── App.js
│   │   └── __tests__/
│   │       └── integration.test.js
│   ├── package.json
│   └── Dockerfile
├── sq-e2e/             # Pruebas End-to-End (Playwright)
│   ├── tests/
│   │   └── e2e.spec.js
│   └── package.json
└── docker-compose.sq.yml
```

## Componentes

### sq-db (MySQL)
Base de datos que almacena las tareas.

### sq-be (Backend Python/Flask)
API REST que expone endpoints:
- `GET /tasks` - Obtener todas las tareas
- `POST /tasks` - Crear una nueva tarea
- `GET /health` - Health check

### sq-fe (Frontend React)
Interfaz de usuario para gestionar tareas.

## Niveles de Pruebas de Integración

### Nivel 1: Backend ↔ Database
**Archivo:** `sq-be/tests_integration/test_be_db_integration.py`

Pruebas que validan la comunicación entre el backend y la base de datos:
- `BE-INT-01`: Insertar tarea y verificar que se puede leer
- `BE-INT-02`: Leer tareas y verificar estructura JSON

### Nivel 2: Frontend ↔ Backend
**Archivo:** `sq-fe/src/__tests__/integration.test.js`

Pruebas que validan la comunicación entre el frontend y el backend:
- `FE-INT-01`: Frontend obtiene y renderiza tareas del backend real

### Nivel 3: End-to-End (E2E)
**Archivo:** `sq-e2e/tests/e2e.spec.js`

Pruebas que simulan un usuario real usando Playwright:
- `E2E-01`: Flujo completo - POST via UI → GET via UI

## Ejecución Manual

### Terminal 1: Levantar servicios base
```bash
docker compose -f docker-compose.sq.yml up sq-db sq-be
```

### Terminal 2: Pruebas de integración Backend
```bash
cd sq-be
pytest tests_integration -v
```

### Terminal 3: Pruebas de integración Frontend
```bash
cd sq-fe
npm test -- src/__tests__/integration.test.js --watchAll=false
```

### Terminal 4 (Opcional): Pruebas E2E
```bash
cd sq-e2e
npx playwright test
```

## Ejecución Automatizada con Docker

Ejecutar todas las pruebas de integración automáticamente:

```bash
docker compose -f docker-compose.sq.yml up --build sq-be-int-tests sq-fe-int-tests
```

Para ejecutar todos los servicios incluyendo frontend:

```bash
docker compose -f docker-compose.sq.yml up --build
```

Luego acceder a:
- Frontend: http://localhost:3000
- Backend API: http://localhost:5000
- Health Check: http://localhost:5000/health

## Casos de Prueba Implementados

### Backend ↔ Database
1. ✅ Insertar tarea y verificar lectura
2. ✅ Leer tareas y verificar estructura
3. ✅ Validar POST con datos inválidos
4. ✅ Insertar y leer múltiples tareas

### Frontend ↔ Backend
1. ✅ Frontend obtiene y renderiza tareas del backend real
2. ✅ Verificar que se muestran elementos de lista
3. ✅ Verificar estado de carga inicial

### End-to-End
1. ✅ Usuario ve tareas end-to-end
2. ✅ Usuario puede agregar nueva tarea via UI
3. ✅ Lista de tareas se muestra correctamente

## Notas Importantes

- **Sin mocks**: Las pruebas de integración usan comunicación real entre componentes
- **Base de datos real**: Se usa MySQL real, no in-memory
- **Backend real**: El frontend se conecta al backend real, no a mocks
- **E2E real**: Playwright simula un navegador real

## Requisitos

- Docker y Docker Compose
- Node.js 18+ (para desarrollo local)
- Python 3.11+ (para desarrollo local)
- npm/pip (para desarrollo local)

