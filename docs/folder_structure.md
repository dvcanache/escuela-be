# Project Structure: Modular Monolith (DDD-Oriented)

This structure ensures strict domain isolation while allowing the application to run as a single unit. Each module is self-contained, facilitating a future split into microservices.

```text
com.escuela
├── Core (Cross-cutting concerns)
│   ├── config/             # Global configurations (JPA, Security, Swagger)
│   ├── security/           # JWT filters, Authentication Entry Points
│   ├── exception/          # Global Exception Handler and custom errors
│   └── common/             # Shared Utilities, Base Classes, Enums
│
├── Modules
│   ├── Tenant (Multi-tenancy Management)
│   │   ├── api/            # REST Controllers, DTOs (Requests/Responses)
│   │   ├── application/    # Business logic (Services)
│   │   ├── domain/         # Entities, Repository Interfaces, Value Objects
│   │   └── infrastructure/ # Repository implementations, External integrations
│   │
│   ├── Security (User & Role Management)
│   │   ├── api/
│   │   ├── application/
│   │   ├── domain/
│   │   └── infrastructure/
│   │
│   ├── Schedule (Academic Planning)
│   │   ├── api/
│   │   ├── application/
│   │   ├── domain/
│   │   └── infrastructure/
│   │
│   ├── Attendance (Student Presence)
│   │   ├── api/
│   │   ├── application/
│   │   ├── domain/
│   │   └── infrastructure/
│   │
│   ├── AccessControl (Physical Hardware Logs)
│   │   ├── api/
│   │   ├── application/
│   │   ├── domain/
│   │   └── infrastructure/
│   │
│   └── Notification (Omnichannel Alerts)
│       ├── api/
│       ├── application/
│       ├── domain/
│       └── infrastructure/
```
