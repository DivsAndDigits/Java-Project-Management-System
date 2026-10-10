# Online Project Management Tool

A professional, enterprise-ready web-based project management tool built from the ground up using **Java 17+**, **Jakarta Servlets**, **JSP (JavaServer Pages)**, **JSTL**, and **JDBC**. 

This application demonstrates a clean, layered web architecture **without Spring Boot or Spring MVC**, showcasing native Java EE / Jakarta EE principles, object-oriented software design, and database persistence.

---

## 📋 Table of Contents
- [Project Purpose](#-project-purpose)
- [Technologies Used](#-technologies-used)
- [Architecture & Layering](#-architecture--layering)
- [Academic Requirement: Servlet-Based Web Layer & MVC Architecture](#-academic-requirement-servlet-based-web-layer--mvc-architecture)
- [Academic OOP Principles in the Codebase](#-academic-oop-principles-in-the-codebase)
- [Academic Requirement: Collections & Generics (6 Marks)](#-academic-requirement-collections--generics-6-marks)
- [Academic Requirement: Multithreading & Synchronization (4 Marks)](#-academic-requirement-multithreading--synchronization-4-marks)
- [Academic Requirement: Classes for Database Operations (7 Marks)](#-academic-requirement-classes-for-database-operations-7-marks)
- [Folder Structure](#-folder-structure)
- [UI & Navigation Modules](#-ui--navigation-modules)
- [How to Run the Project](#-how-to-run-the-project)
- [Database Schema Blueprint](#-database-schema-blueprint)
- [Future Roadmap & Upcoming Modules](#-future-roadmap--upcoming-modules)

---

## 🎯 Project Purpose

The **Online Project Management Tool** provides software teams, product managers, and technical leads with a centralized platform to:
- Monitor sprint velocity and project milestones through a high-impact executive dashboard.
- Create, track, and budget enterprise projects across departments.
- Assign actionable work items, prioritize bugs and features, and track due dates.
- Analyze team resource allocation and workload capacity.
- Receive in-app notifications and sprint update alerts.
- Maintain a secure, decoupled, and testable codebase adhering to SOLID design principles.

---

## 🛠 Technologies Used

| Technology | Specification / Version | Role in Project |
| :--- | :--- | :--- |
| **Java** | OpenJDK 17+ (Tested on Java 17 to 26) | Core backend programming language |
| **Build Tool** | Apache Maven 3.8+ | Dependency management and `.war` artifact packaging |
| **Web Container** | Apache Tomcat 10.1+ | Jakarta EE 10 / Servlet 6.0 compatible runtime |
| **Servlets** | Jakarta Servlet API 6.0 | Request dispatching, session handling, HTTP routing |
| **Presentation** | Jakarta Server Pages (JSP) 3.1 & JSTL 3.0 | Server-side templating and dynamic view rendering |
| **Database** | MySQL 8.x & MySQL Connector/J 8.3.0 | Relational database persistence ready |
| **Connectivity** | Pure JDBC (`java.sql.*`) | Native statement execution, connection lifecycle |
| **Frontend** | HTML5, CSS3 (Modern Flexbox/Grid), JavaScript (ES6+) | SaaS-grade responsive dashboard UI |
| **Testing** | JUnit 5 (`junit-jupiter`) | Unit testing of domain models and service layers |

> **Note:** Spring Boot and Spring MVC are intentionally excluded to showcase pure Jakarta Servlet, JSP, and JDBC architecture.

---

## 🏗 Architecture & Layering

The application follows a strict **Layered Architecture** pattern ensuring separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                 Client Browser (HTML5 / CSS3 / JS)          │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP Requests / UTF-8
┌──────────────────────────────▼──────────────────────────────┐
│  Filter Layer (CharacterEncodingFilter, LoggingFilter, ...) │
└──────────────────────────────┬──────────────────────────────┘
                               │ Dispatches to
┌──────────────────────────────▼──────────────────────────────┐
│  Controller Layer (Jakarta Servlets: DashboardServlet, ...) │
└──────────────────────────────┬──────────────────────────────┘
                               │ Invokes
┌──────────────────────────────▼──────────────────────────────┐
│  Service Layer (ProjectService, TaskService, UserService...) │
└──────────────────────────────┬──────────────────────────────┘
                               │ Uses Generic DAO Interfaces
┌──────────────────────────────▼──────────────────────────────┐
│  DAO Layer (BaseDao<T,ID>, ProjectDao, TaskDao, UserDao...) │
└──────────────┬──────────────────────────────┬───────────────┘
               │ (Current Phase)              │ (Next Phase)
┌──────────────▼─────────────┐ ┌──────────────▼───────────────┐
│ In-Memory Thread-Safe Data │ │ Pure JDBC + MySQL Driver     │
│ (ConcurrentHashMap & Atomics)│ │ (java.sql.Connection / Pool) │
└────────────────────────────┘ └──────────────────────────────┘
```

1. **`controller/`**: Jakarta Servlets (`@WebServlet`) that receive HTTP requests, parse and validate form parameters, orchestrate services, and forward responses to JSP views or issue redirects.
2. **`model/`**: Serializable domain models (`Project`, `Task`, `User`, `Admin`, `ProjectManager`, `TeamMember`, `Notification`, `DashboardStats`, `ReportResult`) and type-safe enums (`Role`, `ProjectStatus`, `TaskStatus`, `Priority`).
3. **`dao/`**: Data Access Object layer featuring `CrudOperations<T, ID>` and `BaseDao<T, ID>` interfaces, with `AbstractInMemoryDao<T, ID>` implementing common in-memory CRUD operations.
4. **`service/`**: Business logic layer managing validations, cross-entity relationships, metrics aggregation, polymorphic `ReportGenerator` engines, and service singletons via `ServiceFactory`.
5. **`util/`**: Centralized utilities including `DatabaseConnectionManager` (thread-safe Singleton for JDBC connections), `DateTimeUtils`, `ValidationUtils`, and `FormatUtils`.
6. **`exception/`**: Custom exception hierarchy rooted in `ApplicationException` (`ProjectNotFoundException`, `TaskNotFoundException`, `InvalidUserException`, `DatabaseOperationException`, `UnauthorizedAccessException`).
7. **`filter/`**: Servlet filters (`@WebFilter`) enforcing UTF-8 request/response encoding, performance latency logging, and session user checks.
8. **`views/`**: Secure server-side templates stored under `WEB-INF/views/` (inaccessible to direct browser URL tampering) with reusable layout partials (`header.jsp`, `sidebar.jsp`, `navbar.jsp`, `footer.jsp`).

---

## 🌐 Academic Requirement: Servlet-Based Web Layer & MVC Architecture

The presentation and web controller tier is implemented purely with **Jakarta Servlets 6.0**, **JSP 3.1**, and **JSTL 3.0**, strictly adhering to the Model-View-Controller (MVC) architectural pattern:

```
Browser Request
      │
      ▼
AuthenticationFilter (/*) ──[Unauthenticated]──> Redirect /login
      │ [Authenticated or Public]
      ▼
Jakarta Servlet (Controller: LoginServlet, DashboardServlet, ProjectServlet...)
      │ Invokes
      ▼
Service Layer (UserService, ProjectService, TaskService...)
      │ Invokes
      ▼
DAO Layer (JdbcUserDao, JdbcProjectDao, JdbcTaskDao...)
      │ PreparedStatement
      ▼
MySQL Database
      │ Returns Domain Models
      ▼
Jakarta Servlet (Binds models to request.setAttribute("...", model))
      │ RequestDispatcher.forward(request, response)
      ▼
JSP Template (View Presentation Only: dashboard.jsp, projects.jsp, tasks.jsp...)
```

### 1. Servlets & HTTP Operation Mapping

| Servlet Class | URL Pattern | HTTP Methods | Core Responsibilities |
| :--- | :--- | :--- | :--- |
| **`LoginServlet`** | `/login` | `GET`, `POST` | **`GET`**: Renders `login.jsp` (or redirects if already authenticated).<br/>**`POST`**: Validates credentials via `UserService.loginUser()`, creates `HttpSession`, stores `currentUser`, and redirects to `/dashboard`. |
| **`LogoutServlet`** | `/logout` | `GET`, `POST` | Invalidates active `HttpSession` via `session.invalidate()` and redirects to `/login?loggedOut=true`. |
| **`DashboardServlet`** | `/dashboard`, `""` | `GET` | Aggregates KPI statistics via `DashboardService.getDashboardStatistics()`, binds `stats`, `projects`, and `users` to request attributes, and forwards to `dashboard.jsp`. |
| **`ProjectServlet`** | `/projects` | `GET`, `POST` | **Create Project**: `POST /projects` with name, code, budget, priority.<br/>**List Projects**: `GET /projects` with filtering, sorting, and search.<br/>**View Project**: `GET /projects?action=view&id=...`<br/>**Update Project**: `POST /projects?action=update&id=...`<br/>**Delete Project**: `POST/GET /projects?action=delete&id=...` |
| **`TaskServlet`** | `/tasks` | `GET`, `POST` | **Create Task**: `POST /tasks` with title, project, assignee, estimated hours.<br/>**List Tasks**: `GET /tasks` with status, project, priority, and overdue filters.<br/>**Assign Task**: `POST/GET /tasks?action=assign&id=...&assigneeId=...`<br/>**Update Status**: `POST/GET /tasks?action=status&id=...&newStatus=...`<br/>**Delete Task**: `POST/GET /tasks?action=delete&id=...` |
| **`TeamServlet`** | `/team` | `GET` | Retrieves team workload distribution and directory via `UserService.getTeamMemberSummaries()` and forwards to `team.jsp`. |
| **`NotificationServlet`** | `/notifications` | `GET`, `POST` | Lists notifications for the current authenticated user, marks notifications read (`action=markRead`, `action=markAllRead`), and triggers background sweeps (`action=checkDeadlines`). |
| **`ProfileServlet`** | `/profile` | `GET`, `POST` | **`GET`**: Displays logged-in user profile, managed projects, and assigned work.<br/>**`POST`**: Updates user details via `UserService.updateUser()`. |
| **`UserServlet`** | `/users` | `GET`, `POST` | User administration: find by ID, update user details, delete user. |
| **`ReportServlet`** | `/reports` | `GET` | Invokes polymorphic report generators and renders analytical dashboards. |

### 2. Session Management & AuthenticationFilter

- **`AuthenticationFilter` (`/*`)**: Intercepts all incoming requests.
  - **Public Whitelist**: Static resources (`/css/*`, `/js/*`, `/images/*`, `.css`, `.js`, `.ico`, `.png`, `.jpg`, `.svg`) and auth endpoints (`/login`, `/register`, `/logout`) are allowed without an active session.
  - **Protected Enpoints**: Any attempt to access `/dashboard`, `/projects`, `/tasks`, `/team`, `/reports`, `/notifications`, or `/profile` without an active session containing `currentUser` is intercepted and redirected:
    ```
    /login?error=Please+sign+in+to+access+this+page.
    ```
  - **Context Propagation**: For authenticated requests, the filter attaches `currentUser` and the real-time `unreadNotificationCount` to request scope so navigation badges update automatically across all pages.

### 3. Architecture Rules Enforced

1. **Zero SQL in Servlets**: No database statements or `java.sql` types exist in servlets. All persistence flows through `Service -> DAO -> JDBC`.
2. **Zero Business Logic in Servlets**: Business validations, calculations, and rules reside strictly in domain services.
3. **Pure Presentation in JSP**: JSPs contain zero business logic or scriptlets (`<% %>`). Presentation uses JSTL (`<c:forEach>`, `<c:if>`, `<c:choose>`) and Expression Language (`${...}`).
4. **Graceful Exception Handling**: Servlets catch `ValidationException`, `ResourceNotFoundException`, and `UnauthorizedAccessException` cleanly, redirecting with URL-encoded user-friendly messages rather than exposing stack traces or database errors.

---

## 🎓 Academic OOP Principles in the Codebase

This project strictly exemplifies the 6 core pillars of Object-Oriented Programming:

### 1. Encapsulation
- **Private Fields & Data Hiding**: Every domain entity (`User`, `Admin`, `ProjectManager`, `TeamMember`, `Project`, `Task`) encapsulates its state behind `private` variables.
- **Defensive Invariant Validation**:
  - `User.setEmail(String email)` enforces valid RFC-compliant email formats and throws `InvalidUserException`.
  - `User.setUsername(String username)` rejects blank or null usernames.
  - `Project.setBudget(double budget)` and `Project.setSpentAmount(double spent)` disallow negative values.
  - `Project.setProgressPercentage(int progress)` guarantees bounds within `0% - 100%`.
  - `Task.setEstimatedHours(int hours)` ensures non-negative work item duration.

### 2. Inheritance
- **User Role Hierarchy**:
  ```
  User (Abstract Base Class)
   ├── Admin (System Administration, Security Auditing, Superuser)
   ├── ProjectManager (Milestone Scoping, Budget Thresholds, Resource Allocation)
   └── TeamMember (Technical Specialization, Sprint Velocity, Task Execution)
  ```
- **DAO Hierarchy**:
  - `CrudOperations<T, ID>` ──► `BaseDao<T, ID>` ──► `ProjectDao`, `TaskDao`, `UserDao`, `NotificationDao`
  - `AbstractInMemoryDao<T, ID>` ──► `InMemoryProjectDao`, `InMemoryTaskDao`, `InMemoryUserDao`, `InMemoryNotificationDao`
- **Report Generator Hierarchy**:
  - `ReportGenerator` (Interface) ──► `AbstractReportGenerator` (Template Base)
    ├── `ExecutiveSummaryReportGenerator`
    ├── `FinancialBurnRateReportGenerator`
    └── `SprintVelocityReportGenerator`

### 3. Polymorphism
- **Dynamic Method Dispatch in Authorization**:
  - Abstract methods `canManageProjects()`, `canAssignTasks()`, and `hasAdministrativePrivileges()` defined in `User` are polymorphically resolved at runtime:
    - In `ProjectServiceImpl.createProject(Project)`: `manager.canManageProjects()` executes either `Admin.canManageProjects()` or `ProjectManager.canManageProjects()`, dynamically rejecting `TeamMember` with an `UnauthorizedAccessException`.
    - In `TaskServiceImpl.createTask(Task)`: `reporter.canAssignTasks()` dynamically verifies authorization.
- **Polymorphic Analytical Reports**:
  - `ReportServiceImpl` maintains a `List<ReportGenerator>`. Invoking `generateConsolidatedReports()` loops through the collection and executes `generator.generate(stats)` polymorphically.
- **Polymorphic Notification Channels**:
  - `NotificationServiceImpl` maintains a `List<NotificationChannel>` (`InAppNotificationChannel`, `EmailNotificationChannel`, `AuditLogNotificationChannel`) and dispatches alerts via dynamic dispatch.

### 4. Abstraction
- **Abstract Classes**:
  - `model.User`: Captures the core essence of a system participant without allowing direct instantiation. Concrete roles specify distinct behaviors.
  - `service.AbstractReportGenerator`: Defines the template method `generate(DashboardStats)` enforcing execution flow while leaving `compileMetrics()` as an abstract hook.
  - `dao.AbstractInMemoryDao<T, ID>`: Provides common thread-safe generic CRUD functionality while delegating entity ID extraction via abstract hooks.

### 5. Interfaces
- **Generic CRUD Contracts**:
  - `dao.CrudOperations<T, ID>`: Generic contract declaring standard persistence operations using Java Generics (`<T, ID>`).
  - `dao.BaseDao<T, ID>`: Extends `CrudOperations<T, ID>` for DAO specialization.
- **Domain Service Interfaces**:
  - `service.ReportGenerator`: Interface for report computation engines.
  - `service.NotificationService`: Interface for notification delivery.
  - `service.NotificationChannel`: Interface for polymorphic alert channels.
  - `service.ProjectService`, `service.TaskService`, `service.UserService`, `service.DashboardService`.

### 6. Exception Handling
- **Custom Exception Hierarchy**:
  ```
  ApplicationException (Unchecked Base Runtime Exception)
   ├── ResourceNotFoundException
   │    ├── ProjectNotFoundException
   │    └── TaskNotFoundException
   ├── ValidationException
   │    └── InvalidUserException
   ├── DatabaseException
   │    └── DatabaseOperationException
   ├── AuthenticationException
   └── UnauthorizedAccessException
  ```
- **Information Leakage Protection**:
  - Controllers (`ProjectServlet`, `TaskServlet`, `ProfileServlet`, `ReportServlet`) catch domain exceptions and render user-safe messages, while securely logging internal stack traces and causes to the server console. Database credentials and technical SQL error strings are strictly hidden from clients.

---

## 💎 Academic Requirement: Collections & Generics (6 Marks)

The project incorporates comprehensive, non-trivial usage of Java Collections and Generics across all layers:

| Requirement Component | Architectural Location | Academic Implementation Detail |
| :--- | :--- | :--- |
| **1. Generic DAO & Repository** | `dao.CrudOperations<T, ID>`, `dao.BaseDao<T, ID>`, `dao.AbstractInMemoryDao<T, ID>` | Standardized type-safe CRUD operations (`findById`, `findAll`, `findAllAsMap`, `save`, `update`, `deleteById`, `existsById`, `count`) without code duplication. |
| **2. List Collection** | `model.Project`, `model.Task`, `service.ProjectService`, `service.TaskService` | Ordered retrieval and display of project portfolios (`List<Project>`), task milestone items (`List<Task>`), and team feeds, consumed directly by Servlets and JSP views. |
| **3. Set Collection** | `model.Project` (`teamMembers`), `service.ProjectService` | `Set<User>` prevents duplicate team member assignment within projects. Enforces mathematical set uniqueness via properly overridden `User.equals()` and `User.hashCode()`. |
| **4. Map Collection** | `dao.AbstractInMemoryDao`, `service.TaskService`, `service.ProjectService`, `model.DashboardStats` | Fast O(1) primary key lookups (`Map<Long, Project>`, `Map<Long, Task>`, `Map<Long, User>`) and dynamic grouping metrics (`Map<TaskStatus, Long>`, `Map<Priority, Long>`, `Map<ProjectStatus, Long>`). |
| **5. Sorting** | `service.ProjectServiceImpl`, `service.TaskServiceImpl` | Dynamic multi-attribute sorting using `Comparator.comparing()` and `thenComparing()`. Tasks sort by deadline (`dueDate`), priority, status, or estimated hours; Projects sort by progress, priority, or deadline. |
| **6. Filtering** | `service.ProjectServiceImpl`, `service.TaskServiceImpl` | Java Stream predicate filtering by status, priority, assignee ID, project ID, and overdue date criteria (`dueDate < today && status != COMPLETED`). |
| **7. Searching** | `service.ProjectServiceImpl`, `service.TaskServiceImpl`, `util.GenericCollectionUtils` | Case-insensitive keyword search across project name, code, description, task title, and task description, integrated directly with web page search bars. |
| **8. Generic Utility** | `util.GenericCollectionUtils` | Reusable generic methods (`filter`, `sort`, `search`, `groupBy`, `countBy`, `indexBy`, `toSet`) leveraging bounded wildcards with the PECS principle (`? super T`, `? extends K`). |
| **9. Thread-Safe Collections** | `dao.AbstractInMemoryDao` (`ConcurrentHashMap`), `model.Project` (`CopyOnWriteArraySet`) | Multi-threaded Servlet container safety. `ConcurrentHashMap` avoids race conditions during concurrent HTTP requests, while `CopyOnWriteArraySet` prevents `ConcurrentModificationException` during concurrent reads. |
| **10. Dashboard/Report Integration** | `model.DashboardStats`, `service.DashboardServiceImpl`, `dashboard.jsp`, `reports.jsp` | Real-time aggregated Map and Set statistics rendered dynamically via JSTL `<c:forEach var="entry" items="${stats.tasksByStatus}">` and `<c:forEach var="entry" items="${stats.tasksByPriority}">`. |

---

## ⚡ Academic Requirement: Multithreading & Synchronization (4 Marks)

The project incorporates native Java Concurrency, thread synchronization, thread-safe shared resources, and background task scheduling without external frameworks:

| Multithreading Component | Implementation Class | Academic Concurrency Detail |
| :--- | :--- | :--- |
| **1. Background Task Scheduling** | `service.concurrent.DeadlineReminderTask` | Periodic `Runnable` background task scanning tasks for overdue and approaching deadlines without blocking user HTTP requests. |
| **2. Scheduled Executor Management** | `service.concurrent.AppSchedulerManager` | Controls `ScheduledExecutorService` with custom daemon `ThreadFactory` (`OPMT-DeadlineChecker`), periodic execution (`scheduleAtFixedRate`), and immediate sweep triggers. |
| **3. Mutual Exclusion & Synchronization** | `service.concurrent.NotificationCoordinator` | Guards shared dispatch state with `ReentrantLock` and Double-Checked Locking, preventing check-then-act race conditions and duplicate notifications. |
| **4. Thread-Safe Shared Collections** | `ConcurrentHashMap.newKeySet()`, `ConcurrentHashMap`, `CopyOnWriteArraySet` | Scalable memory visibility and thread-safe collections avoiding coarse locks and immune to `ConcurrentModificationException`. |
| **5. Container Lifecycle Listener** | `listener.ApplicationLifecycleListener` | Jakarta `@WebListener` / `ServletContextListener` starting the scheduler on startup (`contextInitialized`) and cleanly shutting it down on teardown (`contextDestroyed`) preventing thread leaks. |
| **6. Concurrent Progress Update Safety** | `model.Project.recordTaskCompletion()` | `synchronized` atomic methods on domain entity monitor locks preventing lost updates when multiple threads finish tasks concurrently. |
| **7. Multi-Threaded Batch Processing** | `service.concurrent.NotificationBatchProcessor` | Worker pool (`ExecutorService`, 4 threads) concurrently evaluating batches of tasks across parallel workers using `Callable<Void>` and `invokeAll()`. |
| **8. Web Application Integration** | `controller.NotificationServlet`, `notifications.jsp` | Asynchronously created deadline and overdue alerts appear seamlessly in user feeds and navbar badges with on-demand manual scan button (`⚡ Check Deadlines`). |

### Race Condition Analysis & Synchronization Strategy

#### 1. Check-Then-Act Race Condition (Duplicate Notifications)
- **Problem**: When a scheduled background sweep runs at the same time an asynchronous batch processor or user request executes, two threads could evaluate an overdue task simultaneously. Both check `dispatchedEventKeys.contains(key)` at the exact same instant; both observe `false`. Both proceed to instantiate and persist duplicate notifications, creating duplicate alerts for the same event.
- **Solution**: [`NotificationCoordinator.java`](file:///c:/Java%20project/src/main/java/service/concurrent/NotificationCoordinator.java) wraps the check, creation, and key registration inside a critical section locked with `ReentrantLock`. Double-checked locking ensures only the winning thread generates the alert, while subsequent threads are intercepted and counted as `duplicatesSuppressed`.

#### 2. Lost Update Race Condition (Concurrent Progress Recalculation)
- **Problem**: When two developers complete separate tasks in the same project concurrently, worker threads in Tomcat could both read `completedTasks = 8`, both compute `completedTasks + 1 = 9`, and both write `9`, losing an update (should be `10`).
- **Solution**: [`Project.recordTaskCompletion()`](file:///c:/Java%20project/src/main/java/model/Project.java) is `synchronized` on the project instance monitor, guaranteeing atomic read-modify-write execution and consistent progress percentages across all threads.

---

## 🗄 Academic Requirement: Classes for Database Operations (7 Marks)

The project includes an enterprise-grade persistence architecture built directly with **native JDBC (`java.sql.*`)** and **MySQL 8.x**, demonstrating complete separation of concerns and robust data access patterns without external ORMs (like Hibernate) or frameworks (like Spring Data).

### 1. Architectural Flow
```
Servlet (Controller)
    ↓ invokes
Service Layer (Business Logic & Validations)
    ↓ accesses
DAO Interface (Generic CRUD Contract: CrudOperations<T, ID> & BaseDao<T, ID>)
    ↓ implemented by
JDBC DAO Implementation (PreparedStatement, SQL Joins, ResultSet mapping)
    ↓ acquires java.sql.Connection from
DatabaseConnectionManager (Singleton, externalized db.properties, leak-free cleanup)
    ↓ executes against
MySQL 8.x Database (Database: project_management)
```

### 2. Classes for Database Operations Mapping

| Academic Deliverable | Core Class / Interface | Academic Responsibility & Marks Justification |
| :--- | :--- | :--- |
| **Central JDBC Connection Manager** | [`util.DatabaseConnectionManager`](file:///c:/Java%20project/src/main/java/util/DatabaseConnectionManager.java) | Thread-safe Singleton managing JDBC lifecycle. Externalizes credentials via `db.properties`, provides `getConnection()`, `isDatabaseAvailable()`, and `closeQuietly()` for leak-free resource teardown. |
| **User Persistence & Polymorphism** | [`dao.JdbcUserDao`](file:///c:/Java%20project/src/main/java/dao/JdbcUserDao.java) | Full CRUD via `PreparedStatement`. Maps relational table rows into polymorphic Java subclasses (`Admin`, `ProjectManager`, `TeamMember`) dynamically based on the `role` column. |
| **Project Persistence & Joins** | [`dao.JdbcProjectDao`](file:///c:/Java%20project/src/main/java/dao/JdbcProjectDao.java) | Full CRUD using SQL `LEFT JOIN` on `users` to eagerly hydrate manager details. Automatically loads project team members via `team_members` join table. |
| **Work Item & Task Persistence** | [`dao.JdbcTaskDao`](file:///c:/Java%20project/src/main/java/dao/JdbcTaskDao.java) | Full CRUD using multi-table SQL `LEFT JOIN` on `projects` and `users` (assignee and reporter). Supports overdue query filtering, priority ordering, and keyword search. |
| **Team Member Association & Constraints** | [`dao.JdbcTeamMemberDao`](file:///c:/Java%20project/src/main/java/dao/JdbcTeamMemberDao.java) | Manages Many-to-Many associations in the `team_members` join table. Enforces Set uniqueness and duplicate prevention via composite `PRIMARY KEY (project_id, user_id)`. |
| **Notification Persistence** | [`dao.JdbcNotificationDao`](file:///c:/Java%20project/src/main/java/dao/JdbcNotificationDao.java) | Full CRUD managing in-app notifications. Provides unread count calculations and atomic `markAsRead` / `markAllAsRead` updates. |
| **In-Memory & JDBC Coexistence** | [`service.ServiceFactory`](file:///c:/Java%20project/src/main/java/service/ServiceFactory.java) | Dynamic Service Locator toggling between `InMemory` DAOs (for fast unit testing) and `JDBC` DAOs (for production MySQL persistence) based on `dao.mode` configuration. |

### 3. Database Schema Blueprint (`src/main/resources/schema.sql`)
The MySQL schema uses database **`project_management`** with strict referential integrity:
- **`users`**: Primary Key `id`, Unique `username` and `email`, role, department, plus polymorphic subclass attributes (`admin_level`, `is_superuser`, `budget_approval_limit`, `certification_level`, `technical_specialization`, `sprint_velocity`).
- **`projects`**: Primary Key `id`, Unique `code`, Foreign Key `manager_id` referencing `users(id)` (`ON DELETE SET NULL`), budget, progress, schedule dates.
- **`team_members`**: Composite Primary Key `(project_id, user_id)`, Foreign Keys referencing `projects(id)` and `users(id)` (`ON DELETE CASCADE`). Prevents duplicate project-member relationships at the database engine level.
- **`tasks`**: Primary Key `id`, Foreign Key `project_id` (`ON DELETE CASCADE`), `assignee_id` (`ON DELETE SET NULL`), `reporter_id` (`ON DELETE SET NULL`), priority, status, due date, estimated/logged hours.
- **`notifications`**: Primary Key `id`, Foreign Key `user_id` (`ON DELETE CASCADE`), alert type, read status, action link, creation timestamp.
- **Indexes**: Applied on frequently filtered and joined columns (`idx_projects_manager`, `idx_tasks_project`, `idx_tasks_assignee`, `idx_tasks_status`, `idx_tasks_due_date`, `idx_notifications_user_read`).

### 4. ACID Transaction Management
Multi-step related operations are protected using manual JDBC transactions to guarantee atomicity:
- **Implementation**: [`JdbcProjectDao.createProjectWithTeam(Project, Set<Long>)`](file:///c:/Java%20project/src/main/java/dao/JdbcProjectDao.java)
- **Workflow**:
  ```java
  Connection conn = connectionManager.getConnection();
  try {
      conn.setAutoCommit(false); // Begin ACID Transaction
      // Step 1: Insert project into 'projects' table & retrieve generated key
      ...
      // Step 2: Batch insert initial team assignments into 'team_members' table
      ...
      conn.commit(); // Commit all changes atomically
  } catch (SQLException e) {
      if (conn != null) {
          conn.rollback(); // Rollback all changes on failure to maintain consistency
      }
      throw new DatabaseOperationException("Transaction failed: rolled back project creation", e);
  } finally {
      if (conn != null) {
          conn.setAutoCommit(true);
          conn.close(); // Cleanly return connection
      }
  }
  ```

### 5. SQL Injection Safety & Resource Management
1. **100% Parameterized Queries**: Every SQL query across `JdbcUserDao`, `JdbcProjectDao`, `JdbcTaskDao`, `JdbcNotificationDao`, and `JdbcTeamMemberDao` utilizes `java.sql.PreparedStatement` with indexed parameter placeholders (`?`). String concatenation of user inputs is strictly prohibited, neutralizing SQL injection vectors.
2. **Leak-Free Resource Teardown**: All JDBC operations leverage Java 7+ `try-with-resources` ensuring that `java.sql.Connection`, `java.sql.PreparedStatement`, and `java.sql.ResultSet` are closed deterministically in reverse order of acquisition.
3. **User-Safe Exception Handling**: Technical low-level `java.sql.SQLException` errors are caught at the DAO boundary and translated into [`DatabaseOperationException`](file:///c:/Java%20project/src/main/java/exception/DatabaseOperationException.java). This protects sensitive schema metadata and credentials from leaking to the presentation layer or end users.

### 6. Externalized Configuration (`src/main/resources/db.properties`)
```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/project_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=root
db.password=password

# Persistence DAO mode: "memory" or "jdbc"
dao.mode=memory
```
Passwords and connection parameters are externalized from Java source code. The application seamlessly runs in `memory` mode for fast unit tests, or `jdbc` mode for live MySQL database connectivity.

---

## 📂 Folder Structure

```
c:/Java project/
├── pom.xml                                  # Maven project configuration (war packaging)
├── README.md                                # Project documentation & technical guide
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── controller/                  # Jakarta HTTP Servlets
│   │   │   │   ├── AuthServlet.java         # /login and /register user authentication
│   │   │   │   ├── UserServlet.java         # /users user lookup & deletion
│   │   │   │   ├── DashboardServlet.java    # /dashboard and root (/) view handler
│   │   │   │   ├── ProjectServlet.java      # /projects listing, filters, and creation
│   │   │   │   ├── TaskServlet.java         # /tasks listing, filters, and creation
│   │   │   │   ├── TeamServlet.java         # /team workload & directory
│   │   │   │   ├── ReportServlet.java       # /reports analytical overview
│   │   │   │   ├── NotificationServlet.java # /notifications feed & status updates
│   │   │   │   ├── ProfileServlet.java      # /profile view & update form
│   │   │   │   └── LogoutServlet.java       # /logout session invalidation
│   │   │   ├── model/                       # Domain Entities, DTOs & Enums
│   │   │   │   ├── Role.java                # Enum: ADMIN, PROJECT_MANAGER, TEAM_MEMBER, CLIENT
│   │   │   │   ├── ProjectStatus.java       # Enum: PLANNING, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED
│   │   │   │   ├── TaskStatus.java          # Enum: TODO, IN_PROGRESS, IN_REVIEW, COMPLETED, BLOCKED
│   │   │   │   ├── Priority.java            # Enum: LOW, MEDIUM, HIGH, URGENT
│   │   │   │   ├── User.java                # [Abstraction] Abstract User base entity
│   │   │   │   ├── Admin.java               # [Inheritance] Administrator specialized subtype
│   │   │   │   ├── ProjectManager.java      # [Inheritance] Project Manager specialized subtype
│   │   │   │   ├── TeamMember.java          # [Inheritance] Team Member specialized subtype
│   │   │   │   ├── Project.java             # Project entity with schedule, budget, metrics
│   │   │   │   ├── Task.java                # Work item entity with priority & assignee
│   │   │   │   ├── TeamMemberSummary.java   # DTO for workload & capacity analytics
│   │   │   │   ├── Notification.java        # In-app alert entity
│   │   │   │   ├── DashboardStats.java      # Aggregated metrics container DTO
│   │   │   │   └── ReportResult.java        # Structured analytical report findings DTO
│   │   │   ├── dao/                         # Data Access Object Interfaces & Implementations
│   │   │   │   ├── CrudOperations.java      # [Interface] Generic CRUD contract with Generics
│   │   │   │   ├── BaseDao.java             # Specialization extending CrudOperations<T, ID>
│   │   │   │   ├── AbstractInMemoryDao.java # [Abstraction] Thread-safe in-memory base DAO
│   │   │   │   ├── ProjectDao.java          # Project DAO interface
│   │   │   │   ├── TaskDao.java             # Task DAO interface
│   │   │   │   ├── UserDao.java             # User DAO interface
│   │   │   │   ├── NotificationDao.java     # Notification DAO interface
│   │   │   │   ├── TeamMemberDao.java       # Team member association DAO contract
│   │   │   │   ├── InMemoryProjectDao.java  # Thread-safe mock implementation
│   │   │   │   ├── InMemoryTaskDao.java     # Thread-safe mock implementation
│   │   │   │   ├── InMemoryUserDao.java     # Thread-safe mock implementation
│   │   │   │   ├── InMemoryNotificationDao.java # Thread-safe mock implementation
│   │   │   │   ├── InMemoryTeamMemberDao.java # Thread-safe mock team member implementation
│   │   │   │   ├── JdbcUserDao.java         # [JDBC] MySQL User DAO with PreparedStatement
│   │   │   │   ├── JdbcProjectDao.java      # [JDBC] MySQL Project DAO with Joins & Transactions
│   │   │   │   ├── JdbcTaskDao.java         # [JDBC] MySQL Task DAO with Joins & Overdue logic
│   │   │   │   ├── JdbcTeamMemberDao.java   # [JDBC] MySQL Team Member DAO with UNIQUE constraints
│   │   │   │   └── JdbcNotificationDao.java # [JDBC] MySQL Notification DAO with read updates
│   │   │   ├── service/                     # Business Logic Layer
│   │   │   │   ├── ProjectService.java      # Project service contract
│   │   │   │   ├── ProjectServiceImpl.java  # Project business logic
│   │   │   │   ├── TaskService.java         # Task service contract
│   │   │   │   ├── TaskServiceImpl.java     # Task business logic
│   │   │   │   ├── UserService.java         # User & team service contract
│   │   │   │   ├── UserServiceImpl.java     # User business logic
│   │   │   │   ├── DashboardService.java    # Dashboard metrics service contract
│   │   │   │   ├── DashboardServiceImpl.java# Consolidated statistics calculation
│   │   │   │   ├── ReportService.java       # Analytical reporting service contract
│   │   │   │   ├── ReportServiceImpl.java   # Coordinates polymorphic report generators
│   │   │   │   ├── ReportGenerator.java     # [Interface] Contract for report engines
│   │   │   │   ├── AbstractReportGenerator.java # [Abstraction] Template base for report generators
│   │   │   │   ├── ExecutiveSummaryReportGenerator.java # [Polymorphism] Executive report generator
│   │   │   │   ├── FinancialBurnRateReportGenerator.java# [Polymorphism] Financial analytics generator
│   │   │   │   ├── SprintVelocityReportGenerator.java   # [Polymorphism] Velocity diagnostics generator
│   │   │   │   ├── NotificationService.java # [Interface] Notification service contract
│   │   │   │   ├── NotificationServiceImpl.java # Coordinates polymorphic notification channels
│   │   │   │   ├── NotificationChannel.java # [Interface] Alert dispatch channel abstraction
│   │   │   │   ├── InAppNotificationChannel.java    # [Polymorphism] In-app channel
│   │   │   │   ├── EmailNotificationChannel.java    # [Polymorphism] Email channel
│   │   │   │   ├── AuditLogNotificationChannel.java # [Polymorphism] Audit trail channel
│   │   │   │   ├── concurrent/              # Multithreading & Background Processing
│   │   │   │   │   ├── DeadlineReminderTask.java # Background scheduled task checking deadlines
│   │   │   │   │   ├── NotificationCoordinator.java # ReentrantLock synchronized dispatch coordinator
│   │   │   │   │   ├── NotificationBatchProcessor.java # Multi-threaded parallel batch worker pool
│   │   │   │   │   └── AppSchedulerManager.java # ScheduledExecutorService lifecycle manager
│   │   │   │   └── ServiceFactory.java      # Service Locator & Singleton wiring
│   │   │   ├── listener/                    # Web Application Lifecycle Listeners
│   │   │   │   └── ApplicationLifecycleListener.java # Starts/stops background executors
│   │   │   ├── util/                        # Shared Utilities
│   │   │   │   ├── DBConnection.java        # [JDBC] Centralized connection & close utility
│   │   │   │   ├── DatabaseConnectionManager.java # JDBC connection manager (Singleton)
│   │   │   │   ├── GenericCollectionUtils.java # Reusable generic utility (PECS bounded wildcards)
│   │   │   │   ├── DateTimeUtils.java       # Date formatting & relative time helpers
│   │   │   │   ├── ValidationUtils.java     # Input validation & XSS sanitization
│   │   │   │   └── FormatUtils.java         # Currency & percentage formatting
│   │   │   ├── exception/                   # Custom Exception Hierarchy
│   │   │   │   ├── ApplicationException.java# Base runtime application exception
│   │   │   │   ├── ResourceNotFoundException.java # Base 404 resource lookup failure
│   │   │   │   ├── ProjectNotFoundException.java  # Missing project entity
│   │   │   │   ├── TaskNotFoundException.java     # Missing task work item
│   │   │   │   ├── ValidationException.java # Form & parameter validation failure
│   │   │   │   ├── InvalidUserException.java# Invalid user properties or email
│   │   │   │   ├── DatabaseException.java   # Base persistence & SQL wrapper
│   │   │   │   ├── DatabaseOperationException.java# Sanitized database failure
│   │   │   │   ├── AuthenticationException.java   # Security authentication failure
│   │   │   │   └── UnauthorizedAccessException.java# Permission & authorization denial
│   │   │   └── filter/                      # Jakarta Web Filters
│   │   │       ├── CharacterEncodingFilter.java # Enforces UTF-8 encoding
│   │   │       ├── AuthenticationFilter.java    # Session verification & unread count
│   │   │       └── LoggingFilter.java       # HTTP request latency logger
│   │   ├── resources/
│   │   │   ├── db.properties                # MySQL connection configuration template
│   │   │   └── schema.sql                   # MySQL 8.x schema DDL blueprint
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   ├── web.xml                  # Jakarta EE 10 deployment descriptor
│   │       │   └── views/                   # Server-side JSP views
│   │       │       ├── common/
│   │       │       │   ├── header.jsp       # Meta tags, fonts, styles
│   │       │       │   ├── sidebar.jsp      # Responsive navigation sidebar
│   │       │       │   ├── navbar.jsp       # Top search, modal trigger, user menu
│   │       │       │   └── footer.jsp       # Modal dialogs & scripts
│   │       │       ├── login.jsp            # Authentication sign-in form
│   │       │       ├── register.jsp         # User registration form
│   │       │       ├── dashboard.jsp        # Executive dashboard view
│   │       │       ├── projects.jsp         # Projects portfolio & filter table
│   │       │       ├── tasks.jsp            # Sprint work items & priority pills
│   │       │       ├── team.jsp             # Team directory & workload progress
│   │       │       ├── reports.jsp          # Analytics & budget burn charts
│   │       │       ├── notifications.jsp    # Activity feed & alerts
│   │       │       └── profile.jsp          # User settings & assigned tasks
│   │       ├── css/
│   │       │   └── style.css                # SaaS dashboard design system
│   │       ├── js/
│   │       │   └── main.js                  # Modals, mobile drawer, filters & toasts
│   │       └── images/
│   │           └── logo.svg                 # Brand SVG logo
│   └── test/
│       └── java/
│           ├── controller/
│           │   ├── EndToEndApplicationWorkflowTest.java   # End-to-end servlet workflow tests
│           │   ├── MockServletSupport.java                 # In-memory mock servlet context & requests
│           │   └── ServletWebLayerIntegrationTest.java     # Web layer integration tests
│           ├── dao/
│           │   ├── EndToEndJdbcVerificationTest.java       # Comprehensive JDBC persistence verification
│           │   ├── JdbcDaoArchitectureTest.java            # JDBC tests (PreparedStatements, transactions, mock verification)
│           │   └── JdbcDatabaseIntegrationTest.java        # Live MySQL Integration Tests (JUnit 5 Assumptions)
│           └── service/
│               ├── AdvancedFeaturesAndSecurityTest.java    # PBKDF2, CSRF, security headers, RBAC & IDOR tests
│               ├── CollectionsAndGenericsTest.java         # List, Set, Map, Sorting, Filtering tests
│               ├── MultithreadingAndSynchronizationTest.java # Concurrency & thread safety tests
│               ├── OopConceptsIntegrationTest.java         # Comprehensive academic OOP verification tests
│               └── ProjectServiceTest.java                 # JUnit 5 test cases
```

---

## 🎨 UI & Navigation Modules

The user interface follows a modern SaaS design language (inspired by tools like Linear, Jira, and Asana):

1. **Dashboard (`/dashboard` or `/`)**:
   - Four KPI cards displaying **Active Projects**, **Tasks In Progress**, **Overdue Items**, and **Team Capacity**.
   - Recent Projects table with live completion percentage progress bars and status badges.
   - High Priority Work Items table with urgent indicator badges and assignee avatars.
   - Side panel with milestone progress widgets and team allocation bars.
2. **Projects (`/projects`)**:
   - Filter pills for *All*, *In Progress*, *Planning*, *On Hold*, and *Completed*.
   - Direct modal button to register a new project (name, code, budget, priority, deadline).
   - Responsive table tracking progress, budget vs. spent, and team members.
3. **Tasks (`/tasks`)**:
   - Filter pills for *All*, *To Do*, *In Progress*, *In Review*, and *Completed*.
   - Actionable work items table displaying estimated vs. logged hours, priority pills, and due dates.
4. **Team (`/team`)**:
   - Card grid for each team member displaying avatar initials, role badge, department, active project count, and workload capacity progress bar.
5. **Reports (`/reports`)**:
   - High-level financial tracking (Total Approved Budget, Current Spend, Burn Rate %).
   - Task completion rates and delivery health indicators.
6. **Notifications (`/notifications`)**:
   - Activity feed with urgency-coded badges (`info`, `warning`, `danger`, `success`).
   - One-click "Mark as Read" and "Mark All as Read" actions.
7. **Profile (`/profile`)**:
   - Read-only role permission review and editable profile form (full name, email, department).
   - List of personal tasks currently assigned to the user.
8. **Logout (`/logout`)**:
   - Invalidates the HTTP session and safely redirects to the dashboard with a notification.

---

## 🚀 How to Run the Project

### Prerequisites
- **Java Development Kit (JDK 17 or higher)** installed (`java -version`).
- **Apache Tomcat 10.1+** (compatible with Jakarta EE 9/10 / Servlet 6.0).
- **Apache Maven 3.8+** (or use IntelliJ IDEA / Eclipse bundled Maven).

### Option 1: Running with Maven & Apache Tomcat (Command Line)
1. **Build the WAR file**:
   ```bash
   mvn clean package
   ```
   This compiles all Java classes, executes JUnit 5 tests, and generates `target/project-management-tool.war`.

2. **Deploy to Apache Tomcat 10.1+**:
   - Copy `target/project-management-tool.war` into your Tomcat installation's `webapps/` directory:
     ```bash
     copy target\project-management-tool.war C:\apache-tomcat-10.1.x\webapps\
     ```
   - Start Tomcat:
     ```bash
     C:\apache-tomcat-10.1.x\bin\startup.bat
     ```

3. **Access the application**:
   - Open your browser at:
     ```
     http://localhost:8080/project-management-tool/
     ```

---

### Option 2: Running in IntelliJ IDEA (Ultimate or Community)
1. Open IntelliJ IDEA and choose **Open**, selecting the `c:\Java project` directory.
2. IntelliJ will automatically detect the `pom.xml` and configure dependencies.
3. **Run Configuration**:
   - Go to **Run** → **Edit Configurations...**
   - Click **+** → Select **Tomcat Server** → **Local** (or install the free **Smart Tomcat** plugin).
   - Set Tomcat Home to your local Tomcat 10.1+ installation directory.
   - Under the **Deployment** tab, add `online-project-management-tool:war` with Application Context `/` or `/project-management-tool`.
   - Click **Run** or **Debug**.

---

### Option 3: Running in Eclipse IDE for Enterprise Java
1. Open Eclipse → **File** → **Import...** → **Existing Maven Projects**.
2. Select `c:\Java project` and click **Finish**.
3. In the **Servers** view, right-click → **New** → **Server** → select **Apache Tomcat v10.1 Server**.
4. Right-click the server → **Add and Remove...** → add `online-project-management-tool` to configured resources.
5. Right-click the server → **Start**.
6. Open your browser at `http://localhost:8080/online-project-management-tool/`.

---

## 🗄 Database Setup & MySQL 8.x Configuration

The application implements full native **MySQL 8.x** database persistence via pure JDBC (`java.sql.*`) without Spring Boot or ORM frameworks, adhering strictly to enterprise and academic standards.

### 1. Centralized JDBC Connection Architecture

Native JDBC connectivity is centralized through [`src/main/java/util/DBConnection.java`](file:///c:/Java%20project/src/main/java/util/DBConnection.java) and [`src/main/java/util/DatabaseConnectionManager.java`](file:///c:/Java%20project/src/main/java/util/DatabaseConnectionManager.java):

```
Servlet  ──►  Service  ──►  DAO Interface  ──►  Concrete JDBC DAO  ──►  DBConnection / PreparedStatement  ──►  MySQL 8.x
```

- **MySQL JDBC Driver**: `com.mysql.cj.jdbc.Driver` dynamically loaded via `Class.forName()`.
- **`DriverManager`**: Standard JDBC driver manager dispensing active connection handles.
- **`Connection`**: Manages underlying database sessions and ACID transaction demarcations (`setAutoCommit(false)`, `commit()`, `rollback()`).
- **`PreparedStatement`**: Parameterized query execution exclusively across every SQL statement to safeguard against SQL injection vulnerabilities.
- **`ResultSet`**: Relational row cursor mapped into strongly-typed domain entities and polymorphic subtypes (`Admin`, `ProjectManager`, `TeamMember`).
- **`SQLException`**: Captured at the DAO boundary and translated into the sanitized, user-safe `DatabaseOperationException`.
- **`try-with-resources`**: Automatic resource management ensuring all `Connection`, `PreparedStatement`, and `ResultSet` handles are cleanly closed.

#### Key Methods in `DBConnection`:
```java
// Acquire active connection from centralized manager
Connection conn = DBConnection.getConnection();

// Acquire connection with custom credentials
Connection conn = DBConnection.getConnection(url, username, password);

// Probe database availability
boolean active = DBConnection.isConnected();

// Safely close one or more JDBC resources without throwing
DBConnection.close(resultSet, preparedStatement, connection);
```

---

### 2. Implemented Database Operations Matrix

All database interactions are implemented end-to-end through the layered pipeline (`Servlet -> Service -> DAO -> JDBC -> MySQL`):

| Domain | Operation | Servlet Endpoint | Service Method | DAO Method | SQL Query Pattern |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Users** | Register User | `POST /register` | `UserService.registerUser` | `UserDao.register` | `INSERT INTO users (...) VALUES (?, ?, ...)` |
| | Login User | `POST /login` | `UserService.loginUser` | `UserDao.login` | `SELECT * FROM users WHERE ... password = ?` |
| | Find by ID | `GET /users?id={id}` | `UserService.getUserById` | `UserDao.findById` | `SELECT * FROM users WHERE id = ?` |
| | Update User | `POST /users?action=update` | `UserService.updateUser` | `UserDao.update` | `UPDATE users SET ... WHERE id = ?` |
| | Delete User | `POST /users?action=delete` | `UserService.deleteUser` | `UserDao.deleteById` | `DELETE FROM users WHERE id = ?` |
| **Projects** | Create Project | `POST /projects` | `ProjectService.createProject` | `ProjectDao.save` | `INSERT INTO projects (...) VALUES (?, ?, ...)` |
| | Retrieve Projects | `GET /projects` | `ProjectService.getAllProjects` | `ProjectDao.findAll` | `SELECT p.*, u.* FROM projects p LEFT JOIN users u ...` |
| | Retrieve Project by ID | `GET /projects?action=view` | `ProjectService.getProjectById` | `ProjectDao.findById` | `SELECT p.*, u.* FROM projects p ... WHERE p.id = ?` |
| | Update Project | `POST /projects?action=update` | `ProjectService.updateProject` | `ProjectDao.update` | `UPDATE projects SET ... WHERE id = ?` |
| | Delete Project | `POST /projects?action=delete` | `ProjectService.deleteProject` | `ProjectDao.deleteById` | `DELETE FROM projects WHERE id = ?` |
| **Tasks** | Create Task | `POST /tasks` | `TaskService.createTask` | `TaskDao.save` | `INSERT INTO tasks (...) VALUES (?, ?, ...)` |
| | Retrieve Tasks | `GET /tasks` | `TaskService.getAllTasks` | `TaskDao.findAll` | `SELECT t.*, pr.*, a.*, r.* FROM tasks t ...` |
| | Retrieve Task by ID | `GET /tasks?action=view` | `TaskService.getTaskById` | `TaskDao.findById` | `SELECT t.*, pr.*, a.*, r.* FROM tasks t WHERE t.id = ?` |
| | Update Task Status | `POST /tasks?action=status` | `TaskService.updateTaskStatus` | `TaskDao.update` | `UPDATE tasks SET status = ? WHERE id = ?` |
| | Assign Task | `POST /tasks?action=assign` | `TaskService.assignTask` | `TaskDao.assignTask` | `UPDATE tasks SET assignee_id = ? WHERE id = ?` |
| | Delete Task | `POST /tasks?action=delete` | `TaskService.deleteTask` | `TaskDao.deleteById` | `DELETE FROM tasks WHERE id = ?` |
| **Team** | Add Member | `POST /projects?action=addMember` | `ProjectService.addTeamMemberToProject` | `TeamMemberDao.addTeamMember` | `INSERT INTO team_members (project_id, user_id) VALUES (?, ?)` |
| | Remove Member | `POST /projects?action=removeMember` | `ProjectService.removeTeamMemberFromProject` | `TeamMemberDao.removeTeamMember` | `DELETE FROM team_members WHERE project_id = ? AND user_id = ?` |
| | Retrieve Project Members | `GET /projects?action=view` | `ProjectService.getProjectTeamMembers` | `TeamMemberDao.findProjectTeam` | `SELECT u.* FROM users u INNER JOIN team_members tm ...` |
| **Notifications** | Create Notification | `NotificationCoordinator` / Service | `NotificationService.createNotification` | `NotificationDao.save` | `INSERT INTO notifications (...) VALUES (?, ?, ...)` |
| | Retrieve Notifications | `GET /notifications` | `NotificationService.getUserNotifications` | `NotificationDao.findByUserId` | `SELECT * FROM notifications WHERE user_id = ? ...` |
| | Mark as Read | `GET /notifications?action=markRead` | `NotificationService.markNotificationAsRead` | `NotificationDao.markAsRead` | `UPDATE notifications SET is_read = TRUE WHERE id = ?` |

---

### 3. Database Credentials & Configuration Guide

Where to enter your database configuration:

1. Open [`src/main/resources/db.properties`](file:///c:/Java%20project/src/main/resources/db.properties).
2. Enter your MySQL database URL, username, and password:
   ```properties
   # JDBC Driver
   db.driver=com.mysql.cj.jdbc.Driver

   # Connection URL (Database Name: project_management)
   db.url=jdbc:mysql://localhost:3306/project_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC

   # Database Credentials
   db.username=root
   db.password=YourActualPasswordHere

   # Persistence Mode: "jdbc" for live MySQL, "memory" for in-memory collections
   dao.mode=jdbc
   ```
3. **Twelve-Factor Environment Overrides & JVM Properties**:
   The `DatabaseConnectionManager` dynamically supports external environment variables and JVM system properties without modifying `db.properties`:

   | Setting | Environment Variable | JVM System Property | Default Fallback |
   | :--- | :--- | :--- | :--- |
   | JDBC URL | `DB_URL` | `-Ddb.url` | `jdbc:mysql://localhost:3306/project_management?...` |
   | Database User | `DB_USERNAME` or `DB_USER` | `-Ddb.username` | `root` |
   | Database Password | `DB_PASSWORD` | `-Ddb.password` | `password` |
   | Persistence Mode | `DAO_MODE` | `-Ddao.mode` | `jdbc` (or `memory`) |

   Example runtime JVM arguments:
   ```bash
   -Ddb.url="jdbc:mysql://localhost:3306/project_management" -Ddb.username="root" -Ddb.password="secret" -Ddao.mode=jdbc
   ```

> [!IMPORTANT]
> **Database Security Rules**:
> - Database credentials reside strictly in `db.properties` or environment variables on the backend server.
> - **Never** expose database credentials, JDBC URLs, or connection parameters inside JSP files or frontend JavaScript scripts.
> - All technical `SQLException` messages and database schema internals are caught at the DAO boundary and wrapped in user-safe `DatabaseOperationException` before bubbling up.

---

### 4. Database Schema Setup

1. Verify MySQL 8.x is running on `localhost:3306`.
2. Execute the schema script [`src/main/resources/schema.sql`](file:///c:/Java%20project/src/main/resources/schema.sql):
   ```bash
   mysql -u root -p < "c:/Java project/src/main/resources/schema.sql"
   ```
   This provisions the `project_management` schema, relational tables (`users`, `projects`, `team_members`, `tasks`, `notifications`), primary and foreign keys, indexes, and initial seed records.

---

### 5. Pre-Seeded Demo / Test Accounts

The following demo accounts are provisioned via `schema.sql` for instant testing and role-based access verification:

| Username | Password | Role | Assigned Department | Permissions & Capabilities |
| :--- | :--- | :--- | :--- | :--- |
| `sarah.chen` | `password123` | **ADMIN** | DevOps & Cloud | Full administrative capabilities: user management, global dashboard metrics, project and task oversight. |
| `alex.morgan` | `password123` | **PROJECT_MANAGER** | Engineering | Project creation, editing, task assignment, team membership, budget monitoring. |
| `david.kim` | `password123` | **TEAM_MEMBER** | Frontend | Personal assigned task execution, status progression (`TO_DO` → `IN_PROGRESS` → `COMPLETED`). |
| `elena.rostova` | `password123` | **TEAM_MEMBER** | Backend & Core | Backend microservices tasks, status updates, team view. |
| `marcus.vance` | `password123` | **TEAM_MEMBER** | UI/UX Design | Design sprint tasks, status updates, team view. |
| `priya.sharma` | `password123` | **TEAM_MEMBER** | QA & Testing | Test automation & verification tasks, status updates. |

> [!NOTE]
> **Transparent Password Migration**: Upon first login with `password123`, the authentication system verifies the credentials and automatically rehashes and upgrades the stored password to adaptive **PBKDF2WithHmacSHA256** (100,000 iterations, 16-byte cryptographically secure salt) in MySQL without requiring any manual password reset.

---

## 🔮 Roadmap & Implementation Status

- [x] **Foundation & Layout**: Jakarta Servlets 6.0, JSP 3.1, JSTL 3.0, Modern CSS Dashboard.
- [x] **Academic OOP Hierarchy (10 Marks)**: Encapsulation, Polymorphic User subtypes (`Admin`, `ProjectManager`, `TeamMember`), Abstraction, Custom Exceptions.
- [x] **Collections & Generics (6 Marks)**: Generic DAOs (`CrudOperations<T, ID>`, `BaseDao`), `List`, `Set` uniqueness, `Map` lookups, custom filtering & sorting.
- [x] **Multithreading & Synchronization (4 Marks)**: `DeadlineReminderTask` via `ScheduledExecutorService`, `NotificationCoordinator` deduplication with `ReentrantLock`, `ApplicationLifecycleListener`.
- [x] **Database Operations (7 Marks)**: Native JDBC `java.sql.*`, MySQL 8.x (`project_management`), `DBConnection`, `DatabaseConnectionManager`, `JdbcUserDao`, `JdbcProjectDao`, `JdbcTaskDao`, `JdbcNotificationDao`, `JdbcTeamMemberDao`, ACID Transactions (`setAutoCommit`, `commit`, `rollback`), 100% `PreparedStatement` parameterization.
- [x] **JDBC Connectivity (6 Marks)**: Centralized connection manager, driver management, error handling, connection testing, connection isolation.
- [x] **Servlet Web Layer & MVC Architecture (33 Marks)**: Complete MVC workflow (`LoginServlet`, `DashboardServlet`, `ProjectServlet`, `TaskServlet`, `TeamServlet`, `NotificationServlet`, `ProfileServlet`, `UserServlet`, `ReportServlet`), JSP views, JSTL expression language, session management, `AuthenticationFilter`.
- [x] **Security Hardening**: Adaptive PBKDF2 password hashing with legacy migration, CSRF token validation on mutating POST forms, defensive HTTP security response headers (`CSP`, `X-Frame-Options`, `X-Content-Type-Options`, `X-XSS-Protection`), recipient-isolated notification authorization (anti-IDOR), anti-session-fixation on login, HttpOnly cookie configuration, custom error pages suppressing stack traces.
- [x] **Comprehensive Test Verification**: 86 passing automated tests covering OOP, Collections, Concurrency, JDBC, Servlets, and Security.

