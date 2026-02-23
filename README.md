flowchart LR
  %% =========================
  %% External / runtime actors
  %% =========================
  Client[Клієнт (HTTP)]
  Tomcat[Embedded Tomcat (Spring Boot)]
  DS[DispatcherServlet (Spring MVC)]
  DB[(PostgreSQL)]
  Flyway[Flyway migrations\n(db/migration)]
  Storage[(Локальна папка\nstorage/)]

  %% =========================
  %% Application packages
  %% =========================
  subgraph App[com.platon.taskhub]
    subgraph Web[web/]
      Cfg[config/WebRegistrationConfig\n@Bean registrations]
      Flt[filter/CorrelationIdFilter\n(обгортає request/response)]
      Lsn[listener/AppStartupListener\ncreateDirectories(storage/)\nlog: app started]
      HS[servlet/HealthServlet\nGET /health]
      Ctrl[controller/*\nREST controllers]
    end

    subgraph DTO[dto/]
      Req[dto/request/*\nimmutable DTO]
      Resp[dto/response/*\nimmutable DTO]
    end

    subgraph Svc[service/]
      AuthS[AuthService]
      ProjS[ProjectService]
      TaskS[TaskService]
      CommS[CommentService]
      TagS[TagService]
      AttachS[AttachmentService]
    end

    subgraph Audit[audit/]
      Pub[EventPublisher]
      AudL[AuditListener]
    end

    subgraph Repo[repository/ (JDBC)]
      UserR[UserRepository]
      ProjR[ProjectRepository]
      TaskR[TaskRepository]
      CommR[CommentRepository]
      TagR[TagRepository + task_tags]
      AttachR[AttachmentRepository]
      AuditR[AuditLogRepository]
    end

    subgraph Util[util/]
      Hasher[PasswordHasher]
      FileStore[FileStorageService (NIO)]
    end

    subgraph Ex[exception/]
      Err[ErrorResponse\n{code,message,details}]
    end

    Tx[Transaction boundary\n(@Transactional або manual JDBC tx)\nTaskService.createTask/deleteTask]
  end

  %% =========================
  %% Startup flow
  %% =========================
  Start((App startup)) --> Flyway --> DB
  Start --> Lsn --> Storage

  %% =========================
  %% Registration bridge (Spring -> Tomcat)
  %% =========================
  Cfg -->|register filter| Flt
  Cfg -->|register /health servlet| HS
  Cfg -->|register listener| Lsn

  %% =========================
  %% HTTP flow: Spring MVC endpoints
  %% =========================
  Client --> Tomcat --> Flt --> DS --> Ctrl --> Req
  Ctrl --> Svc
  Svc --> Resp --> Ctrl --> DS --> Flt --> Client

  %% Service -> Repository -> DB (typical)
  AuthS --> UserR --> DB
  ProjS --> ProjR --> DB
  CommS --> CommR --> DB
  TagS --> TagR --> DB
  AttachS --> AttachR --> DB

  %% =========================
  %% /health bypasses DispatcherServlet
  %% =========================
  DS -. not used .- HS
  Flt -->|/health| HS --> Flt

  %% =========================
  %% Attachments: DB metadata + filesystem
  %% =========================
  AttachS --> FileStore -->|read/write| Storage

  %% =========================
  %% Audit: event channel (dashed = event)
  %% =========================
  Svc -. publish AuditEvent .-> Pub
  Pub -. event .-> AudL
  AudL --> AuditR --> DB

  %% =========================
  %% Transactions for TaskService create/delete (atomic DB ops)
  %% =========================
  TaskS --> Tx
  Tx --> TaskR --> DB
  Tx --> TagR --> DB
  Tx --> AuditR --> DB
