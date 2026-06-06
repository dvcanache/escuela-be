# Plataforma de Gestión Educativa — Backend

Sistema backend monolítico modular para la administración educativa institucional. Soporta múltiples instituciones (multi-tenancy), control de acceso físico por hardware, gestión académica y notificaciones omnicanal.

---

## Arquitectura

**Patrón:** Monolito Modular (Modular Monolith) con orientación a DDD.

El monolito se despliega como una sola unidad, pero cada dominio está aislado en módulos independientes, facilitando una futura migración a microservicios.

### Pila Tecnológica

| Componente | Tecnología |
|-----------|-----------|
| Lenguaje | Java 17+ |
| Framework | Spring Boot 3.5 |
| Base de Datos | MySQL 8.0 |
| ORM | Spring Data JPA / Hibernate |
| Seguridad | Spring Security + JWT (HS256) |
| Hashing | Argon2id (BC Prov) |
| AOP | Spring AOP (filtro multi-tenant) |
| Build | Maven |
| Contenerización | Docker / Docker Compose |
| Clientes | Angular (Web), Flutter (Móvil) |

---

## Módulos del Sistema

| Módulo | Paquete | Estado | Descripción |
|--------|---------|--------|-------------|
| **Tenant** | `modules/tenant/` | ✅ Implementado | Multi-inquilino, marca blanca (logo, color, dominio) |
| **Security** | `modules/security/` | ✅ Implementado | Autenticación JWT, RBAC, usuarios, roles |
| **Schedule** | `modules/schedule/` | ✅ Implementado | Horarios, asignaturas, validación de colisiones |
| **Attendance** | `modules/attendance/` | 📋 Planeado | Asistencia estudiantil |
| **AccessControl** | `modules/accesscontrol/` | 📋 Planeado | Integración con torniquetes físicos |
| **Notification** | `modules/notification/` | 📋 Planeado | Notificaciones SMTP + WhatsApp Business |

---

## Estructura del Proyecto

```
src/main/java/com/escuela/
├── EscuelaBeApplication.java
├── core/                              # Capa transversal
│   └── common/
│       ├── TenantContext.java         #   ThreadLocal con tenant activo
│       ├── TenantInterceptor.java     #   Interceptor HTTP (X-Tenant-ID)
│       ├── TenantFilterAspect.java    #   AOP: activa filtro Hibernate
│       └── WebConfig.java             #   Registro de interceptores
│
└── modules/
    ├── tenant/
    │   ├── domain/Tenant.java         #   Entidad JPA
    │   └── web/TenantTestController.java
    │
    ├── security/
    │   ├── domain/
    │   │   ├── User.java              #   Entidad JPA (usuarios)
    │   │   ├── Role.java              #   Entidad JPA (roles)
    │   │   └── UserRepository.java    #   Repositorio
    │   ├── web/
    │   │   ├── AuthController.java    #   POST /api/auth/login
    │   │   ├── LoginRequest.java      #   DTO de login
    │   │   ├── JwtResponse.java       #   DTO de respuesta JWT
    │   │   ├── UserTestController.java
    │   │   └── SecurityTestController.java
    │   ├── SecurityConfig.java        #   Spring Security + RBAC
    │   ├── JwtUtils.java              #   Generación/validación JWT
    │   ├── JwtAuthenticationFilter.java # Filtro OncePerRequest
    │   └── UserDetailsServiceImpl.java #   Carga de usuarios
    │
    └── schedule/
        ├── domain/
        │   ├── Schedule.java          #   Entidad JPA con @Filter tenant
        │   └── ScheduleRepository.java
        ├── service/
        │   └── ScheduleService.java   #   Lógica de colisiones
        └── web/
            ├── ScheduleController.java #   CRUD horarios
            ├── ScheduleRequest.java    #   DTO de entrada
            └── ScheduleResponse.java   #   DTO de salida

docs/
├── api.md                             # Documentación de API
├── schema.sql                         # Esquema de base de datos
├── folder_structure.md                # Estructura de carpetas
└── pseudo_code.md                     # Pseudo-código de implementación
```

---

## Ejecución Local

### Requisitos
- Docker y Docker Compose
- Java 17+ (solo para desarrollo sin Docker)

### Usando Docker (recomendado)

```bash
# Construir e iniciar servicios
docker compose up --build -d

# Verificar que el contenedor esté corriendo
docker compose ps

# Ver logs
docker compose logs -f app

# Detener
docker compose down
```

La aplicación estará disponible en `http://localhost:8080`.
La base de datos MySQL estará en `localhost:3307`.

### Sin Docker (desarrollo local)

```bash
# Requiere MySQL corriendo en localhost:3307 (o cambiar en application.properties)
./mvnw spring-boot:run
```

---

## Guía de Inicio Rápido con Postman

Para probar la API, puedes usar Postman siguiendo estos pasos:

### 1. Configuración de Cabeceras (Headers)
Todas las peticiones (excepto las de `/api/test/**`) **deben** incluir estas cabeceras:

| Key | Value | Descripción |
| :--- | :--- | :--- |
| `X-Tenant-ID` | `1` | ID de la institución (ej. Colegio del Norte) |
| `Content-Type` | `application/json` | Necesario para peticiones POST/PATCH |

### 2. Flujo de Autenticación (Login)
La mayoría de las rutas están protegidas. Primero debes obtener un token:

1. **Endpoint:** `POST http://localhost:8080/api/auth/login`
2. **Cuerpo (JSON):**
   ```json
   {
     "username": "admin_norte",
     "password": "admin123"
   }
   ```
3. **Uso del Token:** Copia el valor de `token` de la respuesta. En Postman, ve a la pestaña **Auth**, selecciona **Bearer Token** y pega el código allí.

### 3. Endpoints Principales para Probar

#### Simulación de Hardware (Público)
Simula el paso de un estudiante por un torniquete físico:
* **POST** `/api/hardware/access`
* **Body:** `{"deviceId": "TORN_01", "userId": 3, "direction": "ENTRADA"}`

#### Gestión de Horarios (Admin/Profesor)
* **GET** `/api/admin/schedules` — Lista todos los horarios del tenant.

#### Registro de Asistencia (Admin/Profesor)
* **POST** `/api/admin/attendance`
* **Body:** `{"scheduleId": 1, "studentId": 3, "status": "PRESENTE", "notes": "Llegó puntual"}`

---

## API — Endpoints

### Autenticación

```
POST /api/auth/login
```

```json
// Request
{ "username": "juan.perez", "password": "secreta123" }

// Response 200
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "juan.perez",
  "roles": ["ROLE_PROFESOR"]
}
```

### Endpoints de Desarrollo (sin autenticación)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/test/users` | Lista de usuarios |
| `GET` | `/api/test/security/hash?password=...` | Genera hash Argon2 |
| `GET` | `/api/test/tenant/current` | Muestra tenant activo |

### Horarios (Schedule)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/schedules` | Crear horario |
| `GET` | `/api/schedules` | Listar horarios del tenant |
| `GET` | `/api/schedules/{id}` | Obtener horario por ID |
| `PUT` | `/api/schedules/{id}` | Actualizar horario |
| `PATCH` | `/api/schedules/{id}/cancel` | Cancelar clase |
| `DELETE` | `/api/schedules/{id}` | Eliminar horario |

### Reglas de Acceso (RBAC)

| Ruta | Roles Permitidos |
|------|-----------------|
| `/api/auth/**` | Público |
| `/api/test/**` | Público (dev) |
| `/api/admin/**` | `ADMIN`, `PROFESOR` |
| `/api/mobile/**` | `ESTUDIANTE`, `PADRE` |
| `/api/schedules/**` | `ADMIN`, `PROFESOR` |
| Cualquier otra | Autenticado |

---

## Seguridad

### Autenticación
1. El cliente envía `POST /api/auth/login` con `username` + `password`.
2. El servidor valida contra la base de datos usando `UserDetailsServiceImpl`.
3. Las contraseñas se almacenan con **Argon2id** (no BCrypt).
4. Se genera un token **JWT HS256** con expiración de 24h.
5. El cliente debe enviar el token en cada request: `Authorization: Bearer <token>`.

### Filtro JWT
`JwtAuthenticationFilter` extiende `OncePerRequestFilter`:
- Extrae el token del header `Authorization: Bearer ...`
- Valida firma y expiración usando `JwtUtils`
- Inyecta la autenticación en `SecurityContextHolder`

### Roles del Sistema
`ADMIN`, `PROFESOR`, `ESTUDIANTE`, `PADRE`

---

## Multi-Tenancy

Cada petición debe incluir el header:

```
X-Tenant-ID: 1
```

El flujo de aislamiento es:

1. **`TenantInterceptor`** (HandlerInterceptor) — extrae `X-Tenant-ID` del request HTTP y lo almacena en `TenantContext` (ThreadLocal).
2. **`TenantFilterAspect`** (AOP `@Before`) — antes de ejecutar cualquier método en `*Repository`, activa el filtro `"tenantFilter"` de Hibernate con el tenant ID del contexto.
3. **Hibernate `@Filter`** — las entidades anotadas con `@FilterDef` + `@Filter` (`Schedule`) tienen automáticamente `WHERE tenant_id = :tenantId` en todas las consultas.

---

## Base de Datos

### Tablas

| Tabla | Descripción |
|-------|-------------|
| `tenants` | Instituciones educativas |
| `roles` | Catálogo de roles |
| `users` | Usuarios asociados a un tenant |
| `parent_student_relationship` | Relación padres ↔ estudiantes |
| `schedules` | Horarios académicos |
| `attendance` | Registro de asistencia |
| `access_logs` | Registro de torniquetes |

Ver esquema completo en [`docs/schema.sql`](./docs/schema.sql).

---

## Dependencias Principales (Maven)

| Dependencia | Propósito |
|------------|-----------|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-data-jpa` | Persistencia JPA/Hibernate |
| `spring-boot-starter-security` | Autenticación y control de acceso |
| `spring-boot-starter-validation` | Validación de DTOs con Jakarta Validation |
| `spring-boot-starter-aop` | Aspectos AOP para filtro multi-tenant |
| `jjwt-api / jjwt-impl / jjwt-jackson` | Generación y validación de tokens JWT |
| `mysql-connector-j` | Conector MySQL |
| `bcprov-jdk18on` | Proveedor criptográfico Bouncy Castle (Argon2) |

---

## Variables de Entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3307/escuela_db` | URL de conexión MySQL |
| `SPRING_DATASOURCE_USERNAME` | `root` | Usuario de BD |
| `SPRING_DATASOURCE_PASSWORD` | `root` | Contraseña de BD |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Estrategia DDL de Hibernate |

---

## Documentación Adicional

- [`docs/api.md`](./docs/api.md) — Documentación detallada de la API
- [`docs/pseudo_code.md`](./docs/pseudo_code.md) — Pseudo-código de implementación
- [`docs/schema.sql`](./docs/schema.sql) — Esquema completo de base de datos
- [`docs/folder_structure.md`](./docs/folder_structure.md) — Estructura de carpetas DDD
- [`Informe_Tecnico_Arquitectura_Educativa_v2.txt`](./Informe_Tecnico_Arquitectura_Educativa_v2.txt) — Informe técnico de arquitectura

---

## Licencia

Proyecto privado — Plataforma de Gestión Educativa Institucional.
