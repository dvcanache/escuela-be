# Pseudo-Code de Implementación — Plataforma de Gestión Educativa

Basado en el *Informe Técnico de Desarrollo y Arquitectura v2.0* y la estructura real del proyecto.

---

## 1. Estructura DDD (Monolito Modular)

```
com.escuela/
├── core/                     # Capa Global / Cross-cutting
│   ├── config/               #   Configuraciones globales (JPA, Security)
│   ├── security/             #   Filtros JWT, entry points de autenticación
│   ├── exception/            #   Manejador global de errores
│   └── common/               #   Utilidades compartidas, clases base
│
└── modules/                  # Dominios aislados (cada uno = posible microservicio)
    ├── tenant/               # Multi-Tenant / Marca Blanca
    ├── security/             # Roles, usuarios, autenticación
    ├── schedule/             # Horarios y gestión académica
    ├── attendance/           # Control de asistencia
    ├── accesscontrol/        # Integración con hardware físico
    └── notification/         # Bus de notificaciones (SMTP + WhatsApp)
```

Cada módulo sigue 4 capas internas:
```
module/
├── api/                   # Controladores REST + DTOs
├── application/           # Servicios / casos de uso
├── domain/                # Entidades JPA + interfaces de repositorio
└── infrastructure/        # Implementaciones concretas + adaptadores externos
```

---

## 2. Módulo Tenant (Multi-Inquilino)

### 2.1 Entidad de Dominio

```
Entidad Tenant {
  id: Long (PK, auto-generado)
  name: String (NO NULL)
  domainPrefix: String (ÚNICO, NO NULL)     // ej: "colegio-norte"
  logoUrl: String (opcional, max 2048)
  primaryColor: String (opcional, hex #FFFFFF)
  active: Boolean (default TRUE)
  createdAt: LocalDateTime (solo-lectura, auto-asignado @PrePersist)
}
```

### 2.2 Pseudo-Code — Filtro de Aislamiento Multi-Tenant

```
FUNCTION resolveCurrentTenant():
    // 1. Extraer cabecera "X-Tenant-Id" del request HTTP
    tenantHeader = request.getHeader("X-Tenant-Id")

    IF tenantHeader IS NULL:
        THROW TenantNotResolvedException("Cabecera de tenant requerida")

    // 2. Buscar tenant en BD (cacheados para evitar N+1)
    tenant = tenantRepository.findByDomainPrefix(tenantHeader)

    IF tenant IS NULL OR tenant.active IS FALSE:
        THROW TenantNotFoundException("Tenant inválido o inactivo")

    // 3. Almacenar en ThreadLocal para el resto del request
    TenantContext.setCurrentTenant(tenant.id)

    RETURN tenant.id


// Ejemplo de uso en servicios:
FUNCTION createSchedule(scheduleData):
    tenantId = TenantContext.getCurrentTenant()

    schedule = new Schedule()
    schedule.tenantId = tenantId        // ← Aislamiento por fila
    schedule.professorId = scheduleData.professorId
    schedule.subjectName = scheduleData.subjectName
    schedule.startTime = scheduleData.startTime
    schedule.endTime = scheduleData.endTime
    schedule.status = "ACTIVE"

    RETURN scheduleRepository.save(schedule)
```

---

## 3. Módulo Security (Autenticación y RBAC)

### 3.1 Entidades de Dominio

```
Entidad Role {
  id: Integer (PK)
  name: String (ÚNICO, NO NULL)    // ADMIN | PROFESOR | ESTUDIANTE | PADRE
}

Entidad User {
  id: Long (PK)
  tenant: Tenant (FK → tenants.id, LAZY)
  username: String (ÚNICO por tenant)
  email: String (ÚNICO por tenant)
  passwordHash: String (NO NULL)
  firstName: String
  lastName: String
  role: Role (FK → roles.id, LAZY)
  status: Enum{ACTIVE, INACTIVE, SUSPENDED}
  createdAt: LocalDateTime
  students: Set<User> (M:M vía parent_student_relationship)
  parents: Set<User> (MappedBy, inverso)
}
```

### 3.2 Pseudo-Code — Generación y Validación JWT

```
// GENERACIÓN DE TOKEN (Login)
FUNCTION generateToken(username, tenantId, role):
    claims = new HashMap()
    claims.put("tenantId", tenantId)
    claims.put("role", role.name)

    token = JWT.create()
        .setSubject(username)                       // Identidad del usuario
        .setIssuedAt(Date.from(Instant.now()))       // Emisión
        .setExpiration(Date.from(Instant.now()       // Expira en 24h
            .plus(24, ChronoUnit.HOURS)))
        .addClaims(claims)                          // Claims personalizados
        .signWith(secretKey, Algorithm.HMAC256)     // Firma simétrica

    RETURN token


// FILTRO INTERCEPTOR (Cada request)
FUNCTION doFilterInternal(request, response, filterChain):
    // 1. Extraer token del header "Authorization: Bearer <token>"
    authHeader = request.getHeader("Authorization")

    IF authHeader IS NULL OR !authHeader.startsWith("Bearer "):
        filterChain.doFilter(request, response)     // Pasa al siguiente filtro
        RETURN

    token = authHeader.substring(7)                 // Remueve "Bearer "

    // 2. Validar firma y vigencia
    TRY:
        decodedJWT = JWT.require(secretKey)
            .build()
            .verify(token)
    CATCH JWTVerificationException:
        response.sendError(401, "Token inválido o expirado")
        RETURN

    // 3. Extraer claims
    username = decodedJWT.getSubject()
    tenantId = decodedJWT.getClaim("tenantId").asLong()
    roleName = decodedJWT.getClaim("role").asString()

    // 4. Inyectar identidad en contexto de seguridad
    authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
    authentication = new UsernamePasswordAuthenticationToken(
        username, null, authorities)
    authentication.setDetails(tenantId)              // Tenant asociado
    SecurityContextHolder.getContext().setAuthentication(authentication)

    filterChain.doFilter(request, response)
```

### 3.3 Pseudo-Code — Control de Acceso por Roles (RBAC)

```
// Configuración de seguridad (Spring Security)
FUNCTION configureHttpSecurity(http):
    http
        .csrf().disable()                      // APIs REST sin estado
        .sessionManagement()
            .sessionCreationPolicy(STATELESS)  // Sin sesiones del lado servidor
        .and()
        .authorizeHttpRequests()
            // Rutas públicas
            .requestMatchers("/api/auth/login", "/api/auth/register")
                .permitAll()

            // Portal Administrativo (Angular) → Solo ADMIN y PROFESOR
            .requestMatchers("/api/admin/**")
                .hasAnyRole("ADMIN", "PROFESOR")
            .requestMatchers("/api/schedules/**")
                .hasAnyRole("ADMIN", "PROFESOR")
            .requestMatchers("/api/attendance/**")
                .hasAnyRole("ADMIN", "PROFESOR")

            // App Móvil (Flutter) → ESTUDIANTE y PADRE
            .requestMatchers("/api/student/**")
                .hasAnyRole("ESTUDIANTE", "PADRE")
            .requestMatchers("/api/parent/**")
                .hasRole("PADRE")

            // Hardware → Solo dispositivos autorizados
            .requestMatchers("/api/access-control/**")
                .hasRole("HARDWARE")  // Token interno del dispositivo

            // Todo lo demás → Autenticado
            .anyRequest().authenticated()

    // Filtro JWT ANTES del filtro de seguridad estándar
    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
```

---

## 4. Módulo Schedule (Horarios y Gestión Académica)

### 4.1 Entidad de Dominio

```
Entidad Schedule {
  id: Long (PK)
  tenantId: Long (FK → tenants.id)
  professorId: Long (FK → users.id, rol PROFESOR)
  subjectName: String (NO NULL)
  classroom: String
  startTime: DateTime (NO NULL)
  endTime: DateTime (NO NULL)
  status: Enum{ACTIVE, CANCELLED, FREE_HOUR} (default ACTIVE)
}
```

### 4.2 Pseudo-Code — Validación de Colisiones

```
FUNCTION createOrUpdateSchedule(newSchedule):
    tenantId = TenantContext.getCurrentTenant()

    // Regla de negocio: No colisiones de horario
    // Un profesor no puede estar en 2 clases simultáneas
    // Un aula no puede tener 2 clases simultáneas

    overlappingSchedules = scheduleRepository
        .findConflictingByTenantAndTimeRange(
            tenantId = tenantId,
            professorId = newSchedule.professorId,
            classroom = newSchedule.classroom,
            startTime = newSchedule.startTime,
            endTime = newSchedule.endTime
        )

    // Excluir el mismo registro si es actualización
    IF newSchedule.id IS NOT NULL:
        overlappingSchedules =
            overlappingSchedules.filter(s → s.id != newSchedule.id)

    IF overlappingSchedules IS NOT EMPTY:
        THROW ScheduleConflictException(
            "El aula o profesor ya tiene una clase en este horario: "
            + overlappingSchedules[0].subjectName)

    RETURN scheduleRepository.save(newSchedule)


// Consulta SQL implícita en repositorio:
SQL:
    SELECT * FROM schedules
    WHERE tenant_id = :tenantId
      AND (professor_id = :professorId OR classroom = :classroom)
      AND status = 'ACTIVE'
      AND start_time < :endTime
      AND end_time > :startTime
```

### 4.3 Pseudo-Code — Cancelación de Clase

```
FUNCTION cancelSchedule(scheduleId, reason):
    schedule = scheduleRepository.findById(scheduleId)
        .orElseThrow(() → ScheduleNotFoundException(scheduleId))

    // Solo ADMIN o PROFESOR asignado pueden cancelar
    currentUser = SecurityContext.getCurrentUser()
    IF currentUser.role != "ADMIN"
       AND currentUser.id != schedule.professorId:
        THROW AccessDeniedException("No autorizado para cancelar esta clase")

    schedule.status = "CANCELLED"
    schedule.cancellationReason = reason

    // Disparar evento de dominio → Notificación a estudiantes
    domainEventPublisher.publish(
        new ScheduleCancelledEvent(
            scheduleId = schedule.id,
            professorId = schedule.professorId,
            subjectName = schedule.subjectName,
            startTime = schedule.startTime
        )
    )

    RETURN scheduleRepository.save(schedule)
```

---

## 5. Módulo Attendance (Asistencia Estudiantil)

### 5.1 Entidad de Dominio

```
Entidad Attendance {
  id: Long (PK)
  tenantId: Long (FK → tenants.id)
  scheduleId: Long (FK → schedules.id, NO NULL)
  studentId: Long (FK → users.id, rol ESTUDIANTE)
  presenceStatus: Enum{PRESENTE, AUSENTE, RETARDO, JUSTIFICADO}
  teacherNotes: Text (opcional)
  recordedAt: Timestamp (auto)
}
```

### 5.2 Pseudo-Code — Registro de Asistencia

```
FUNCTION recordAttendanceBatch(scheduleId, attendanceList):
    tenantId = TenantContext.getCurrentTenant()
    currentUser = SecurityContext.getCurrentUser()

    // Validar 1: El horario pertenece al tenant
    schedule = scheduleRepository.findById(scheduleId)
    IF schedule.tenantId != tenantId:
        THROW AccessDeniedException("Horario no pertenece a esta institución")

    // Validar 2: Solo el profesor asignado puede pasar asistencia
    IF currentUser.id != schedule.professorId:
        THROW AccessDeniedException(
            "Solo el profesor asignado puede registrar asistencia")

    // Validar 3: El horario debe estar ACTIVO y ser del día actual
    IF schedule.status != "ACTIVE":
        THROW InvalidScheduleException("Horario cancelado o libre")
    IF schedule.startTime.toLocalDate() != LocalDate.now():
        THROW InvalidScheduleException(
            "Solo se puede registrar asistencia del día actual")

    attendances = []
    FOR EACH entry IN attendanceList:
        // Validar que el estudiante pertenece al mismo tenant
        student = userRepository.findById(entry.studentId)
        IF student.tenantId != tenantId:
            SKIP (log warning)

        attendance = new Attendance()
        attendance.tenantId = tenantId
        attendance.scheduleId = scheduleId
        attendance.studentId = entry.studentId
        attendance.presenceStatus = entry.status
        attendance.teacherNotes = entry.notes

        attendances.add(attendance)

    RETURN attendanceRepository.saveAll(attendances)
```

### 5.3 Pseudo-Code — Modificación de Asistencia (Profesor)

```
FUNCTION updateAttendanceStatus(attendanceId, newStatus, justification):
    attendance = attendanceRepository.findById(attendanceId)
    currentUser = SecurityContext.getCurrentUser()

    // Solo el profesor que registró puede modificar
    schedule = scheduleRepository.findById(attendance.scheduleId)
    IF schedule.professorId != currentUser.id:
        THROW AccessDeniedException("No autorizado")

    // Auditoría: preservar historial en teacherNotes
    oldStatus = attendance.presenceStatus
    attendance.presenceStatus = newStatus
    attendance.teacherNotes = String.format(
        "[%s] Cambio: %s → %s. Motivo: %s",
        LocalDateTime.now(), oldStatus, newStatus, justification)

    RETURN attendanceRepository.save(attendance)
```

---

## 6. Módulo AccessControl (Hardware Físico — Torniquetes)

### 6.1 Entidad de Dominio

```
Entidad AccessLog {
  id: Long (PK)
  tenantId: Long (FK → tenants.id)
  deviceId: String (NO NULL)        // Serial/Código del torniquete
  userId: Long (FK → users.id)
  direction: Enum{ENTRADA, SALIDA}
  eventTimestamp: DateTime (NO NULL) // Timestamp generado por hardware
}
```

### 6.2 Pseudo-Code — Procesamiento de Evento de Torniquete

```
// Endpoint REST de alta velocidad (payload mínimo)
// POST /api/access-control/event
// Body: { "deviceId": "TURNSTILE-01", "cardId": "123456", "timestamp": "..." }

FUNCTION processAccessEvent(deviceId, cardId, eventTimestamp):
    tenantId = TenantContext.getCurrentTenant()

    // 1. Buscar usuario por tarjeta/código de barras
    user = userRepository.findByCardIdAndTenant(cardId, tenantId)
    IF user IS NULL:
        RETURN { granted: false, reason: "Tarjeta no reconocida" }

    // 2. Verificar membresía activa
    IF user.status != "ACTIVE":
        RETURN { granted: false, reason: "Usuario inactivo o suspendido" }

    // 3. Determinar dirección (basada en último evento)
    lastLog = accessLogRepository
        .findTopByUserIdAndTenantOrderByEventTimestampDesc(
            userId = user.id, tenantId = tenantId)
    direction = (lastLog IS NULL OR lastLog.direction == "SALIDA")
        ? "ENTRADA" : "SALIDA"

    // 4. Persistir el log
    log = new AccessLog()
    log.tenantId = tenantId
    log.deviceId = deviceId
    log.userId = user.id
    log.direction = direction
    log.eventTimestamp = eventTimestamp
    accessLogRepository.save(log)

    // 5. Disparar evento de dominio → Notificación (si es ENTRADA)
    IF direction == "ENTRADA":
        domainEventPublisher.publish(
            new StudentEntryEvent(
                studentId = user.id,
                tenantId = tenantId,
                entryTime = eventTimestamp
            )
        )

    RETURN { granted: true, direction: direction }
```

---

## 7. Módulo Notification (Bus de Notificaciones)

### 7.1 Pseudo-Code — Orquestación "Paso → Notificación WhatsApp"

```
// Manejador de evento de dominio (async)
FUNCTION handleStudentEntryEvent(event):
    // 1. Buscar padres vinculados al estudiante
    student = userRepository.findById(event.studentId)
    parents = student.parents                    // M:M desde la entidad User

    // 2. Para cada padre, encolar notificación
    FOR EACH parent IN parents:
        message = String.format(
            "🟢 %s ha ingresado a la institución a las %s.",
            student.firstName + " " + student.lastName,
            event.entryTime.format("HH:mm"))

        notificationQueue.enqueue(NotificationRequest(
            type = "WHATSAPP",
            recipient = parent.phoneNumber,
            message = message
        ))
```

### 7.2 Pseudo-Code — Adaptador WhatsApp Business

```
// Worker de cola de notificaciones
FUNCTION processNotificationQueue():
    WHILE true:
        request = notificationQueue.dequeue()

        SWITCH request.type:
            CASE "EMAIL":
                emailService.send(
                    to = request.recipient,
                    subject = "Notificación Escuela",
                    body = request.message
                )

            CASE "WHATSAPP":
                // Llamada HTTP a API de Meta
                response = httpClient.POST(
                    url = "https://graph.facebook.com/v18.0/" +
                          whatsappPhoneNumberId + "/messages",
                    headers = {
                        "Authorization": "Bearer " + whatsappToken,
                        "Content-Type": "application/json"
                    },
                    body = {
                        "messaging_product": "whatsapp",
                        "to": request.recipient,
                        "type": "text",
                        "text": { "body": request.message }
                    }
                )

                IF response.statusCode != 200:
                    logError("WhatsApp API error: " + response.body)
                    // Re-encolar con backoff exponencial?
                    IF request.retryCount < MAX_RETRIES:
                        request.retryCount++
                        notificationQueue.enqueue(
                            request, delay = 2^request.retryCount * 1000ms)
```

---

## 8. Flujo Extremo a Extremo (End-to-End)

```
1. ADMIN crea horario via Angular
   POST /api/schedules  { tenantId, professorId, subject, start, end }
   → Módulo Schedule valida colisiones → guarda en MySQL

2. Llega el día de la clase. Profesor pasa asistencia.
   POST /api/attendance/batch  { scheduleId, students: [{id, status}] }
   → Módulo Attendance valida: horario activo, profesor titular, hoy
   → Guarda registros en tabla attendance

3. Estudiante pasa por torniquete físico.
   POST /api/access-control/event  { deviceId, cardId, timestamp }
   → Módulo AccessControl busca usuario por tarjeta
   → Verifica estado activo → determina ENTRADA/SALIDA
   → Guarda log → Publica StudentEntryEvent

4. Manejador de evento recibe StudentEntryEvent
   → Busca padres del estudiante en tabla parent_student_relationship
   → Encola notificación WhatsApp

5. Worker de NotificationModule desencola y envía:
   → HTTP POST a API WhatsApp Business de Meta
   → Padre recibe: "🟢 Juan Pérez ha ingresado a las 07:45."
```

---

## 9. Seguridad y Privacidad (Restricción Parent-Estudiante)

```
FUNCTION getStudentAttendanceForParent(parentUserId, studentId):
    currentUser = SecurityContext.getCurrentUser()
    tenantId = TenantContext.getCurrentTenant()

    // Regla: Un PADRE solo ve datos de sus hijos
    parent = userRepository.findById(parentUserId)
    studentIds = parent.students.map(s → s.id)   // Set<Long>

    IF studentId NOT IN studentIds:
        THROW AccessDeniedException(
            "No tiene relación parental con este estudiante")

    // Filtrar por tenant (aislamiento multi-inquilino)
    RETURN attendanceRepository
        .findByTenantIdAndStudentId(tenantId, studentId)
```

---

## 10. Base de Datos — Mapeo Relacional

```
-- 5 tablas principales + 1 tabla asociativa:

tenants (id PK, name, domain_prefix UK, logo_url, primary_color, active)
roles   (id PK, name UK)
users   (id PK, tenant_id FK→tenants, username, email,
         password_hash, first_name, last_name, role_id FK→roles,
         status ENUM, UNIQUE(tenant_id, username),
         UNIQUE(tenant_id, email))

parent_student_relationship (parent_id FK→users, student_id FK→users, PK→compuesta)

schedules  (id PK, tenant_id FK, professor_id FK→users,
            subject_name, classroom, start_time, end_time,
            status ENUM)

attendance (id PK, tenant_id FK, schedule_id FK→schedules,
            student_id FK→users, presence_status ENUM,
            teacher_notes TEXT)

access_logs (id PK, tenant_id FK, device_id, user_id FK→users,
             direction ENUM, event_timestamp)

-- Índices clave:
idx_user_tenant           ON users(tenant_id)
idx_schedule_time         ON schedules(start_time, end_time)
idx_attendance_student    ON attendance(student_id)
idx_access_timestamp      ON access_logs(event_timestamp)
```

---

## 11. Resumen de Capas DDD por Módulo

| Módulo | Domain (Entidad) | API (Controller) | Application (Service) | Infrastructure |
|--------|------------------|------------------|-----------------------|----------------|
| Tenant | `Tenant` | TenantController | TenantService, TenantInterceptor | TenantRepositoryImpl |
| Security | `User`, `Role` | AuthController, UserController | JwtService, AuthService | JwtFilter, CustomUserDetailsService |
| Schedule | `Schedule` | ScheduleController | ScheduleService (collision validation) | ScheduleRepositoryImpl |
| Attendance | `Attendance` | AttendanceController | AttendanceService (integrity rules) | AttendanceRepositoryImpl |
| AccessControl | `AccessLog` | AccessEventController | AccessEventProcessor (turnstile logic) | AccessLogRepositoryImpl |
| Notification | *(event-driven)* | - | NotificationOrchestrator, WhatsAppAdapter | SmtpClient, WhatsAppApiClient |
