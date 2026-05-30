# Documentación de la API - Plataforma de Gestión Educativa

Esta documentación detalla los endpoints disponibles para la integración con los portales Web (Angular) y Móvil (Flutter).

## Configuración General

### Base URL
`http://localhost:8080`

### Cabeceras Obligatorias (Headers)

Todas las peticiones deben incluir la cabecera de inquilino (Tenant) para asegurar el aislamiento de datos:

| Cabecera | Valor | Descripción |
| :--- | :--- | :--- |
| `X-Tenant-ID` | `Long` | ID de la institución (ej. `1`). |
| `Content-Type` | `application/json` | Formato de los datos. |
| `Authorization` | `Bearer <token>` | Token JWT obtenido en el login (requerido para rutas protegidas). |

---

## 1. Módulo de Autenticación (`auth-module`)

### Iniciar Sesión (Login)
Permite obtener el token JWT para acceder a las rutas protegidas.

*   **URL:** `/api/auth/login`
*   **Método:** `POST`
*   **Cuerpo de la Petición:**
    ```json
    {
      "username": "admin_norte",
      "password": "admin123"
    }
    ```
*   **Respuesta Exitosa (200 OK):**
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ...",
      "type": "Bearer",
      "username": "admin_norte",
      "roles": ["ROLE_ADMIN"]
    }
    ```

---

## 2. Módulo de Horarios (`schedule-module`)
*Rutas protegidas para roles: ADMIN, PROFESOR.*

### Crear Horario
Registra una nueva sesión académica validando que no existan colisiones de profesor o aula.

*   **URL:** `/api/admin/schedules`
*   **Método:** `POST`
*   **Cuerpo de la Petición:**
    ```json
    {
      "professorId": 1,
      "subjectName": "Matemáticas",
      "classroom": "A-101",
      "startTime": "2026-06-01T08:00:00",
      "endTime": "2026-06-01T10:00:00"
    }
    ```

### Listar Horarios
Obtiene todos los horarios de la institución (filtrado automáticamente por Tenant).

*   **URL:** `/api/admin/schedules`
*   **Método:** `GET`

### Actualizar Estado de Clase
Permite cancelar una clase o marcarla como hora libre.

*   **URL:** `/api/admin/schedules/{id}/status?status=CANCELLED`
*   **Método:** `PATCH`
*   **Estados Permitidos:** `ACTIVE`, `CANCELLED`, `FREE_HOUR`.

---

## 3. Módulo de Asistencia (`attendance-module`)
*Rutas protegidas para roles: ADMIN, PROFESOR.*

### Registrar Asistencia
El profesor registra la presencia de un estudiante en una clase específica.

*   **URL:** `/api/admin/attendance`
*   **Método:** `POST`
*   **Cuerpo de la Petición:**
    ```json
    {
      "scheduleId": 1,
      "studentId": 3,
      "status": "PRESENTE",
      "notes": "Llegó a tiempo"
    }
    ```
*   **Estados Permitidos:** `PRESENTE`, `AUSENTE`, `RETARDO`, `JUSTIFICADO`.

### Consultar Asistencia por Clase
*   **URL:** `/api/admin/attendance/schedule/{scheduleId}`
*   **Método:** `GET`

---

## 4. Módulo de Control de Acceso (Hardware)
*Ruta pública para simulación de torniquetes.*

### Registrar Paso por Torniquete
Registra el evento físico de entrada o salida y dispara notificaciones a los padres si el usuario es un estudiante.

*   **URL:** `/api/hardware/access`
*   **Método:** `POST`
*   **Cuerpo de la Petición:**
    ```json
    {
      "deviceId": "TORN_01",
      "userId": 3,
      "direction": "ENTRADA"
    }
    ```

---

## Manejo de Errores

El sistema utiliza códigos HTTP estándar para reportar errores:

*   **400 Bad Request:** Error de validación o colisión de negocio (ej. "Conflict: Professor already has a class").
*   **401 Unauthorized:** Token JWT inválido o ausente.
*   **403 Forbidden:** El usuario no tiene el rol necesario para acceder al recurso.
*   **500 Internal Server Error:** Error inesperado en el servidor.
