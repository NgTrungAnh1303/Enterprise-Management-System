# P05 — Equipment Maintenance & Service Scheduling System (EMS)

> A full-stack enterprise web platform for managing the complete equipment maintenance lifecycle — from asset inventory and automated preventive scheduling to incident reporting, work order execution, acceptance inspection, and comprehensive audit logging.

![React](https://img.shields.io/badge/React-18-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=for-the-badge&logo=vite&logoColor=FFFFFF)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=FFFFFF)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-6DB33F?style=for-the-badge&logo=springboot&logoColor=FFFFFF)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=FFFFFF)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=for-the-badge&logo=postgresql&logoColor=FFFFFF)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=FFFFFF)

---

## Overview

**P05-EMS** is a role-based equipment maintenance and service scheduling platform built to digitize and automate the operational workflows of enterprise facility and machinery management.

Instead of treating maintenance as a simple inventory tracking or CRUD ticket system, P05-EMS models the complex, state-driven interactions between equipment operators, field technicians, maintenance managers, and system administrators.

The platform covers:

- Asset inventory registration, type classification, and physical location tracking
- Preventive maintenance plan configuration with automated recurring schedules
- Standardized checklist templates for maintenance procedures
- Incident reporting, triage classification, and severity assignment
- Work order creation, technician assignment, execution, and step-by-step checklists
- Manager inspection, acceptance review, and rejection workflows
- Equipment status transitions (Operating, Under Maintenance, Out of Service)
- Complete immutable audit trails, request ID tracing, and IP rate limiting
- Role-based access control (RBAC) across distinct operational interfaces

The project is developed as an enterprise-grade full-stack application with strict business rule enforcement, finite state machines, and a modern component-driven UI architecture.

---

## Product Workflow

```text
Asset Registration & Location Assignment
                  │
                  ▼
   ┌──────────────────────────────┐
   │ Maintenance Planning Engine  │
   └──────────────┬───────────────┘
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
Preventive Schedule   Incident Report (User)
        │                   │
        │             Manager Triage
        │                   │
        └─────────┬─────────┘
                  │
                  ▼
         Work Order Creation
                  │
                  ▼
       Technician Assignment
                  │
                  ▼
     Checklist & Task Execution
                  │
                  ▼
    Inspection & Result Recording
                  │
                  ▼
       Manager Acceptance Review
         ┌────────┴────────┐
         ▼                 ▼
     [Accepted]       [Rejected]
         │                 │
         ▼                 ▼
   Close Work Order   Rework / Re-dispatch
         │
         ▼
  Equipment Status Update & Audit Logging
```

Each stage strictly enforces permissions, state machine transitions, and data integrity according to the user's role.

---

## Role-based Product Experience

P05-EMS separates responsibilities into specialized workflows rather than a generic single dashboard with conditional buttons.

Each role has an independent information hierarchy, navigation menu, and action set:

| Role | Primary Responsibilities |
|------|---------------------------|
| **Guest / Public** | View landing page, discover system features, self-register account |
| **Equipment User** | Report equipment incidents, request status changes (OOS), track personal incident progress |
| **Technician** | View assigned work orders, execute step-by-step checklists, submit for manager acceptance |
| **Maintenance Manager** | Configure maintenance plans & checklists, triage incident reports, dispatch work orders, review & accept work |
| **Administrator** | Manage user accounts, role approvals, master data (types, locations), inspect audit logs |

---

## Frontend Engineering

The frontend is architected for scalability, responsive usability, and robust state management.

### Authentication-aware Routing & PrivateRoute

Protected application workspaces are strictly separated from public pages using React Router:

- Centralized `AuthContext` managing JWT token persistence and current user principal
- Declarative `PrivateRoute` guarding endpoints based on required roles (`ADMIN`, `MANAGER`, `TECHNICIAN`, `EQUIPMENT_USER`)
- Automatic redirection to designated role dashboards (`DashboardRouterPage`) upon login
- Dedicated error boundaries for `UnauthorizedPage` (401) and `ForbiddenPage` (403)

---

### Finite State Machine-driven Interfaces

Equipment, Incidents, and Work Orders follow strict lifecycle transitions enforced on both frontend and backend:

```text
[Work Order Lifecycle]
DRAFT ──► ASSIGNED ──► IN_PROGRESS ──► COMPLETED ──► ACCEPTED (Closed)
                                           │
                                           └──► REJECTED (Rework)
```

The UI conditionally renders action buttons (e.g. *Start Work*, *Submit for Review*, *Accept*, *Reject*) based on the exact state and user authority, preventing illegal state skips.

---

### Reusable Component Architecture

Core UI interactions are encapsulated into reusable, accessible design tokens:

- **DataTable:** Sortable, paginated data grid with search filter integration
- **ConfirmDialog:** Reusable confirmation modal for high-impact actions (deleting records, status changes)
- **StatusBadge:** Consistent color-coded visual indicator for statuses across modules
- **FilterBar & SearchBar:** Unified search and dropdown filtering pattern
- **PageHeader:** Consistent title, description, and action button toolbar
- **ToastAlert / LoadingState / EmptyState:** Consistent asynchronous UI feedback

---

### API-driven UI States

All asynchronous API interactions follow a structured lifecycle:

```text
Request Initiated
       │
       ├── Loading State (Skeleton / Spinner)
       │
       ├── Success State
       │      ├── Data Available (Interactive Data Table / Form)
       │      └── Empty State (No records found / Friendly illustration)
       │
       └── Error State (Alert Banner with retry trigger)
```

---

# Selected Engineering Challenges

## 01 — Multi-role Separation from a Single Single-Page App

### Problem
Four distinct user groups (Admin, Manager, Technician, User) access the system with vastly different goals. Showing or hiding elements on a shared screen causes cluttered code and accidental authorization leaks.

### Approach
The UI is partitioned into dedicated layout areas (`AppLayout` with role-aware `Sidebar` and `Topbar`) combined with a dynamic routing layer. When a user logs in, `DashboardRouterPage` inspects the user's role and immediately routes them to their specialized dashboard workspace.

---

## 02 — Enforcing Dual-Control & Anti-Self-Acceptance Rules

### Problem
In compliance standards, a manager who assigned a work order or a technician who completed it must not bypass inspection. The same person should not approve their own maintenance without review.

### Approach
Business rules are enforced at both API and UI boundaries. The acceptance modal verifies that the current manager context is authorized to perform acceptance review, and state transitions require valid inspection logs before transitioning from `COMPLETED` to `ACCEPTED`.

---

## 03 — Managing Complex Master-Detail Work Order Executions

### Problem
A single Work Order contains multiple interrelated entities: parent equipment, scheduled checklists, dynamic items to pass/fail, replacement parts, labor hours, and notes.

### Approach
The execution screen (`WorkOrderExecutionPage`) breaks down complex operational inputs into focused cards, auto-calculating completion progress, and validating that every checklist item is acknowledged before allowing submission for manager review.

---

# Architecture

```text
┌─────────────────────────────────────────────────────────┐
│                    React 18 (Vite)                      │
│                                                         │
│   Pages · Components · Routing · Auth Context · State   │
└────────────────────────────┬────────────────────────────┘
                             │
                     REST API / JWT Token
                             │
┌────────────────────────────▼────────────────────────────┐
│                  Nginx Reverse Gateway                  │
│                                                         │
│        SSL Termination · Routing · Rate Limiting        │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│                 Spring Boot 4 / Java 21                 │
│                                                         │
│   REST Controllers · Services · Repositories            │
│   Spring Security · JWT Filter · State Machines · RBAC  │
└────────────────────────────┬────────────────────────────┘
                             │
                      Spring Data JPA
                             │
┌────────────────────────────▼────────────────────────────┐
│                     PostgreSQL 16                       │
│                                                         │
│   Relational Schema · Triggers · Flyway Migrations      │
└─────────────────────────────────────────────────────────┘
```

---

# Technology Stack

## Frontend

| Technology | Purpose |
|------------|---------|
| React 18 | UI component library |
| Vite 5 | High-performance build tool and dev server |
| React Router v6 | Client-side routing and protected routes |
| Bootstrap 5 & React-Bootstrap | Layout, typography, responsive grid |
| Formik & Yup | Form state management and schema validation |
| Chart.js & React-Chartjs-2 | Dashboard operational analytics and KPI charts |

## Backend & Infrastructure

| Technology | Purpose |
|------------|---------|
| Java 21 (LTS) | Modern Java runtime |
| Spring Boot 4.0.6 | Core application framework |
| Spring Security | Authentication, authorization, and CSRF protection |
| JJWT 0.12.5 | Stateless JWT token issuance and verification |
| Spring Data JPA / Hibernate | Object-Relational Mapping (ORM) and data access |
| Flyway | Incremental database schema versioning |
| PostgreSQL 16 | Relational database storage |
| Docker & Docker Compose | Containerized multi-tier deployment |
| Nginx | Gateway reverse proxy and SSL termination |

---

# Project Structure

```text
P05-EMS/
│
├── BE/                                # Backend Application (Spring Boot)
│   ├── src/main/java/com/fpt/ems/be/
│   │   ├── config/                    # Security, OpenAPI, Jackson configurations
│   │   ├── controller/                # REST API controllers
│   │   ├── dto/                       # Data Transfer Objects (Requests & Responses)
│   │   ├── exception/                 # Global exception handler & custom exceptions
│   │   ├── model/                     # JPA domain entities & enums
│   │   ├── repository/                # Spring Data JPA repositories
│   │   ├── security/                  # JWT filter, UserPrincipal, auth policies
│   │   ├── service/                   # Business logic implementations
│   │   ├── statemachine/              # Finite State Machines (WorkOrder, Equipment, Incident)
│   │   └── util/                      # JWT utilities, rate limiters, helpers
│   │
│   ├── src/main/resources/
│   │   ├── db/migration/              # Flyway SQL migrations (V1 to V5)
│   │   ├── application.yaml           # Base application configuration
│   │   └── application-dev.yaml       # Development profile settings
│   │
│   ├── src/test/java/                 # Comprehensive integration and unit test suites
│   ├── Dockerfile                     # Multi-stage backend build
│   └── pom.xml                        # Maven dependency configuration
│
├── FE/                                # Frontend Application (React + Vite)
│   ├── src/
│   │   ├── api/                       # Axios HTTP clients for all backend modules
│   │   ├── assets/                    # Styling tokens and CSS design system
│   │   ├── auth/                      # AuthContext and authentication state provider
│   │   ├── components/                # Reusable common UI components & layout wrappers
│   │   ├── pages/                     # Feature pages grouped by role (admin, manager, tech, user)
│   │   ├── routers/                   # App routes and PrivateRoute protection
│   │   ├── App.jsx                    # Root component
│   │   └── main.jsx                   # React application bootstrap
│   │
│   ├── Dockerfile                     # Nginx production container
│   ├── nginx.conf                     # Client SPA routing config
│   ├── package.json                   # NPM dependencies and scripts
│   └── vite.config.js                 # Vite bundler configuration
│
├── docker/                            # Docker gateway & SSL configuration
├── docker-compose.yml                 # Production multi-tier stack definition
├── docker-compose.dev.yml             # Local development overlay (DB port exposure)
├── .env.example                       # Environment variables template
├── .gitignore                         # Standard git ignore specification
└── README.md
```

---

# Getting Started

## Prerequisites

Make sure the following are installed on your machine:

- **Git:** 2.40+
- **Node.js:** 20.x+
- **JDK:** 21 (LTS)
- **Docker Desktop** (Recommended) or **PostgreSQL 16**

---

## 1. Configure Environment Variables

Create your local `.env` configuration file from the template:

```powershell
copy .env.example .env
```

---

## 2. Start the Database (PostgreSQL 16)

Using Docker Compose:

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d db
```

---

## 3. Start the Backend (Spring Boot)

Open a terminal in the `BE` folder:

```powershell
cd BE
.\mvnw.cmd spring-boot:run
```

- **Backend API:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI Docs:** `http://localhost:8080/v3/api-docs`

---

## 4. Start the Frontend (React + Vite)

Open a separate terminal in the `FE` folder:

```powershell
cd FE
npm install
npm run dev
```

- **Frontend Application:** `http://localhost:5173`

---

# Demo Accounts

The database comes pre-seeded with accounts for all operational roles:

> **Default Password for all accounts:** `Demo@123456`

| Username | Role | Primary Workflow |
|---|---|---|
| **`ADMIN01`** | **ADMIN** | System administration, user/role management, audit log review |
| **`MGR01`** | **MANAGER** | Maintenance plans, technician assignment, work order acceptance |
| **`MGR02`** | **MANAGER** | Secondary manager (for testing dual-control approvals) |
| **`TECH_A`** | **TECHNICIAN** | Work order execution, checklist inspections, acceptance submissions |
| **`TECH_B`** | **TECHNICIAN** | Secondary technician |
| **`USER01`** | **EQUIPMENT_USER** | Incident reporting, equipment status change requests |
| **`USER02`** | **EQUIPMENT_USER** | Secondary user (for testing data privacy) |

---

# Recommended Demo Flow

To walk through the full lifecycle of the platform, follow this sequential flow:

### 1. Equipment User (`USER01`)
- Login and view the **User Dashboard**.
- Submit an **Incident Report** for a malfunctioning equipment item.
- Track real-time progress on **My Incidents**.

### 2. Maintenance Manager (`MGR01`)
- Login and view the **Manager Dashboard**.
- Review and triage the reported incident.
- Create a **Work Order**, attach a standardized checklist, and assign to `TECH_A`.

### 3. Field Technician (`TECH_A`)
- Login and view the **Technician Dashboard**.
- Access the assigned work order on **Work Order List**.
- Start execution, check off procedure items, record observations, and click **Submit for Acceptance**.

### 4. Maintenance Manager (`MGR01`)
- Review completed work on **Work Order Acceptance**.
- Approve and accept the work order.
- Verify equipment status automatically transitions back to `OPERATING`.

### 5. System Administrator (`ADMIN01`)
- Login and view the **Admin Dashboard**.
- Access **Audit Logs** to view immutable cryptographic logs of every state change, user action, and IP trace.
- Inspect **Master Data** and **User Management**.

---

# Engineering Decisions

### React 18 + Vite
Vite provides sub-second hot module replacement (HMR) and optimized rollup production bundles while keeping build configuration clean and maintainable.

### Spring Security + JWT
Stateless token authentication ensures maximum horizontal scalability. Role authorizations are enforced at the method level using pre-authorize annotations and global filter chains.

### Flyway Schema Versioning
All database schema evolution is tracked in reproducible SQL migration files (`V1` to `V5`), eliminating schema drift across local, test, and production environments.

### Finite State Machine Pattern
Equipment statuses, work order progressions, and incident classifications are bounded by explicit state transitions in dedicated state machines, preventing edge-case bugs and race conditions.

---

# Project Context

- **Project:** Equipment Maintenance & Service Scheduling System (EMS)
- **Module:** P05 — Enterprise Application Development & Security
- **Institution:** FPT University
- **Team:**
  - **Trung Anh** (Leader — Core Backend, Security & Architecture)
  - **Trung** (Management & Technician Workflows)
  - **Khương** (User Experience, Authentication & Incident Workflows)
