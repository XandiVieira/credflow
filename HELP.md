# CredFlow

CredFlow is a Spring Boot backend for managing users, accounts, categories, description mappings, and transactions.  
It exposes a secure REST API (JWT-based), persists to PostgreSQL, and ships with Swagger UI.

---

## Table of Contents

- [Requirements](#requirements)
- [Architecture Overview](#architecture-overview)
- [Quick Start (Local)](#quick-start-local)
- [Database Setup](#database-setup)
- [Environment Variables](#environment-variables)
- [Running the App](#running-the-app)
- [API Base URLs & Swagger](#api-base-urls--swagger)
- [Auth Flow (Register → Login → Use token)](#auth-flow-register--login--use-token)
- [Key Endpoints](#key-endpoints)
- [Profiles](#profiles)
- [Docker (optional)](#docker-optional)
- [Build, Test, Package](#build-test-package)
- [Troubleshooting](#troubleshooting)

---

## Requirements

- Java 17+
- Maven 3.9+
- PostgreSQL 14+
- (Optional) Docker / Docker Compose

---

## Architecture Overview

- **Spring Boot** (REST + Validation)
- **Spring Security** with **JWT**
- **JPA/Hibernate** with PostgreSQL
- **springdoc-openapi** (Swagger UI)
- Context path and port are customizable via env vars

---

## Quick Start (Local)

1) **Clone**

```bash
git clone https://github.com/XandiVieira/credflow.git
cd credflow
Start PostgreSQL (use your instance or the compose file below)

Export environment variables (see Environment Variables)

Run

./mvnw spring-boot:run
Database Setup
Option A — Docker Compose
Create docker-compose.yml in the repo root:

services:
  db:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: credflow
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: admin
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d credflow"]
      interval: 5s
      timeout: 3s
      retries: 20
Start it:

docker compose up -d
Option B — Manual PostgreSQL
Create DB and user (adjust as needed):

CREATE DATABASE credflow;
CREATE USER postgres WITH PASSWORD 'admin';
GRANT ALL PRIVILEGES ON DATABASE credflow TO postgres;
If you use a different user/password/host/port, set SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD accordingly.

Environment Variables
Minimum set for local development:

Variable	Required	Example / Default	Notes
SPRING_PROFILES_ACTIVE	✅	local	Chooses profile.
SPRING_DATASOURCE_URL	✅	jdbc:postgresql://localhost:5432/credflow	JDBC URL for Postgres.
SPRING_DATASOURCE_USERNAME	✅	postgres	DB user.
SPRING_DATASOURCE_PASSWORD	✅	admin	DB password.
JWT_SECRET	✅	change-me-please	Strong secret for JWT signing.
JWT_EXPIRATION_MS	✅	7200000	Token lifetime (ms).
PORT	❌	8080	App port (defaults to 8080).

App defaults in application.yml (relevant):

server:
  port: ${PORT:8080}
  servlet:
    context-path: /credflow/api

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger
    operationsSorter: method
    tagsSorter: alpha

management:
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
Security config permits access to /v3/api-docs/**, /swagger/**, /swagger-ui/**, /swagger-ui.html, and public auth endpoints.

Example (Linux/macOS):

export SPRING_PROFILES_ACTIVE=local
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/credflow
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=admin
export JWT_SECRET='change-me-please'
export JWT_EXPIRATION_MS=7200000
export PORT=8080
Example (Windows PowerShell):

$env:SPRING_PROFILES_ACTIVE="local"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/credflow"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="admin"
$env:JWT_SECRET="change-me-please"
$env:JWT_EXPIRATION_MS="7200000"
$env:PORT="8080"

Running the App

./mvnw spring-boot:run
# or
./mvnw -DskipTests package
java -jar target/credflow-*.jar
API Base URLs & Swagger
With the defaults:

Base URL: http://localhost:8080/credflow/api

Swagger UI: http://localhost:8080/credflow/api/swagger

OpenAPI JSON: http://localhost:8080/credflow/api/v3/api-docs

OpenAPI YAML: http://localhost:8080/credflow/api/v3/api-docs.yaml

Health: http://localhost:8080/credflow/api/actuator/health

The /credflow/api context path is important—prefix all routes with it.

Auth Flow (Register → Login → Use token)
Register

curl -X POST http://localhost:8080/credflow/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alex",
    "email": "alex@example.com",
    "password": "StrongP@ssw0rd"
  }'
Login (returns { "token": "..." })

curl -X POST http://localhost:8080/credflow/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alex@example.com",
    "password": "StrongP@ssw0rd"
  }'
Call protected endpoint

TOKEN="paste-jwt-here"
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/credflow/api/v1/accounts
Key Endpoints
Browse Swagger UI for the full list and models.

Auth

POST /v1/auth/register — Register user (public).

POST /v1/auth/login — Obtain JWT (public).

Accounts

GET /v1/accounts

GET /v1/accounts/{id}

PUT /v1/accounts/{id}

DELETE /v1/accounts/{id}

Categories

GET /v1/categories

GET /v1/categories/{id}

POST /v1/categories

PUT /v1/categories/{id}

DELETE /v1/categories/{id}

Description Mappings

GET /v1/description-mappings

GET /v1/description-mappings/{id}

POST /v1/description-mappings (accepts list of DTOs)

PUT /v1/description-mappings/{id}

DELETE /v1/description-mappings/{id}

Transactions

GET /v1/transactions

GET /v1/transactions/{id}

POST /v1/transactions

PUT /v1/transactions/{id}

DELETE /v1/transactions/{id}

Profiles
local: local development (expected to use env-provided datasource).

dev / prod: configure datasources, secrets, logging for your environments.
Set via SPRING_PROFILES_ACTIVE=dev or prod.

Docker (optional)
Build image

./mvnw -DskipTests package
docker build -t credflow .
Run container

docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/credflow \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=admin \
  -e JWT_SECRET=change-me-please \
  -e JWT_EXPIRATION_MS=7200000 \
  credflow
On Linux, replace host.docker.internal with your host IP or run Postgres in another container and put both containers in the same network.

Build, Test, Package
# unit tests
./mvnw test

# run with live reload (DevTools, if added)
./mvnw spring-boot:run

# package runnable jar
./mvnw -DskipTests package
java -jar target/credflow-*.jar
Troubleshooting
404 for endpoints
Ensure you include the context path: /credflow/api.
Example: GET /credflow/api/v1/accounts (not just /v1/accounts).

Swagger not accessible
Visit /credflow/api/swagger. Security allows /v3/api-docs/**, /swagger/**, /swagger-ui/**, /swagger-ui.html.

DB connection refused
Verify Postgres is running and credentials/URL are correct:
SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD.

JWT invalid / 401
Use the exact JWT_SECRET configured for the app and ensure JWT_EXPIRATION_MS hasn’t elapsed.

Port already in use
Change PORT or stop the process bound to that port.

CORS issues
CORS is enabled broadly in SecurityConfig. Adjust allowed origins/headers if needed.









Finance Management App Analysis - CredFlow

  Current State Summary

  Your app is well-architected with solid foundations (7.5/10 overall). The code quality is excellent, following modern Java practices, but there are critical security gaps and missing features.

  ---
  CRITICAL SECURITY ISSUES (Must Fix Immediately)

  1. ✅ Account Isolation Fixed - All endpoints now properly filter by authenticated user's account
  2. CORS Wide Open - Allows all origins (["*"]), should restrict in production
  3. Hardcoded Credentials - DataSeeder creates accounts with password "123456"
  4. ✅ Role-Based Access Implemented - Owner/Member/ReadOnly roles with proper authorities
  5. ✅ JWT Secret Validation - Fails fast on startup if JWT_SECRET is weak, missing, or too short

  ---
  MISSING FEATURES

  High Impact

  - ✅ Budgeting System - Complete with tracking, alerts, rollover, and budget vs actual
  - ✅ Dashboard/Analytics - Full implementation with summary endpoints, trends, and visualizations
  - Recurring Transactions - Type exists but no automation or scheduling
  - ✅ Installment Management - Complete bulk CRUD operations for installment groups
  - ✅ Export Functionality - PDF/Excel/CSV export for all reports
  - ✅ Password Reset Flow Implemented - Email-based token reset with expiration
  - Email Verification - No account verification

  Medium Impact

  - ✅ Credit Card CRUD Complete - All endpoints (CREATE, READ, UPDATE, DELETE) implemented
  - Multi-Currency - Only BRL supported
  - File Attachments - No receipt/invoice upload
  - Notifications - No bill reminders or budget alerts
  - Advanced Reporting - No income vs expense comparison, monthly trends, visualizations

  Low Impact

  - Saved Searches - No search templates
  - ✅ Account Invitations - Invite code workflow implemented (users can join accounts via invite code)
  - ✅ User Preferences - Complete preferences system (theme, language, notifications, profile pictures)
  - ✅ Import History Complete - CSV import with history tracking, audit trail, and rollback capability

  ---
  BUGS & FLAWS

  1. ✅ CreditCardService.findById() - Fixed to throw ResourceNotFoundException consistently
  2. ✅ Refund Detection Race Condition - Synchronized method prevents concurrent state issues
  3. ✅ Category Hierarchy Validation - 2-level limit enforced at service layer
  4. ✅ Soft Deletes Implemented - All entities use deletedAt timestamp with @SQLRestriction
  5. ✅ Optimistic Locking Added - @Version field on BaseEntity prevents concurrent update conflicts
  6. ✅ Audit Trail Complete - createdBy/updatedBy/createdAt/updatedAt tracked via JPA Auditing

  ---
  IMPLEMENTATION SCHEDULE BY PHASE

  PHASE 1: Security & Critical Fixes ✅ COMPLETED

  Priority: CRITICAL - Block production deployment until complete

  1. ✅ Fix account isolation in AccountController.findAll()
  2. ✅ Implement role-based access control (Owner/Member/ReadOnly roles)
  3. Restrict CORS to allowed origins only (TODO: Configure for production)
  4. Remove/profile-gate DataSeeder (TODO: Add profile check)
  5. ✅ Fail-fast on missing JWT_SECRET with validation in @PostConstruct
  6. ✅ Add soft deletes (deletedAt timestamp) to all entities
  7. ✅ Add optimistic locking (@Version) to prevent race conditions
  8. ✅ Fix CreditCardService.findById() exception handling
  9. ✅ Add createdBy/updatedBy audit fields with JPA Auditing

  Deliverables:
  - ✅ Account data properly isolated
  - ✅ Audit trail complete
  - ✅ Role-based access control functional
  - ⚠️  CORS and DataSeeder cleanup pending

  ---
  PHASE 2: Complete Existing Features ✅ COMPLETED

  Priority: HIGH - Fill critical gaps in existing modules

  1. ✅ Complete Credit Card CRUD (UPDATE + DELETE endpoints)
  2. ✅ Implement account invitation workflow (using existing invite codes)
  3. ✅ Add password reset flow (email-based with token expiration)
  4. Add email verification on signup (TODO: Email verification pending)
  5. ✅ Fix refund detection synchronization
  6. ✅ Add category hierarchy validation enforcement
  7. ✅ Implement CSV import with history tracking
  8. ✅ Add import history tracking (CsvImportHistory entity)

  Deliverables:
  - ✅ All CRUD operations complete
  - ✅ User management workflows functional (password reset working)
  - ✅ Import system robust with audit trail
  - ✅ Data integrity enforced
  - ⚠️  Email verification on signup pending

  ---
  PHASE 3: Reporting & Analytics (Week 5-6) 🟢

  Priority: HIGH - Core value proposition

  1. ✅ Create Dashboard Summary Endpoint
    - Total income/expense by period
    - Balance trends over time
    - Top categories breakdown
    - Upcoming bills
  2. ✅ Expense Reports
    - By category (with hierarchy rollup)
    - By responsible user
    - By credit card
    - Month-over-month comparison
  3. ✅ Export Functionality
    - CSV export with filters
    - PDF reports
    - Excel workbooks
  4. ✅ Visualization Data Endpoints
    - Time series for charts
    - Category distribution (pie/donut)
    - Expense trends

  Deliverables:
  - ✅ REST endpoints for dashboard and visualizations
  - ✅ REST endpoints for all reports
  - ✅ Export formats working (CSV, PDF, Excel)
  - ✅ Unit and integration tests complete
  - ✅ Frontend-ready data structures

  ---
  PHASE 4: Budgeting System (Week 7-8) ✅

  Priority: MEDIUM-HIGH - Major feature gap

  1. ✅ Budget Entity & CRUD
    - ✅ Monthly/yearly budgets
    - ✅ Category-level budgets
    - ✅ User-level budgets
    - ✅ Rollover support
  2. ✅ Budget Tracking
    - ✅ Real-time budget vs actual calculation
    - ✅ Percentage consumed
    - ✅ Projected overspend warnings
  3. ✅ Budget Preferences
    - ✅ Configurable thresholds (yellow/orange/red warnings)
    - ✅ Account-level and user-level preferences
    - ✅ Rollover settings (max months, max percentage)
    - ✅ Projected warning settings

  Deliverables:
  - ✅ Complete budgeting module with Budget entity
  - ✅ Budget tracking with rollover calculations
  - ✅ Projected overspend warnings
  - ✅ Configurable preferences system
  - ✅ Integration with existing transactions
  - ✅ Unit and integration tests complete

  ---
  PHASE 5: Recurring Transactions (Week 9) 🔵

  Priority: MEDIUM - Quality of life improvement

  1. Recurrence Pattern Management
    - Daily/weekly/monthly/yearly patterns
    - Custom intervals
    - End date or occurrence count
  2. Automatic Transaction Creation
    - Scheduled job (Spring @Scheduled)
    - Creates transactions based on patterns
    - Handles missed executions
  3. Recurrence CRUD
    - Create recurring template
    - Edit future occurrences
    - Delete with cascade options (this only, future, all)

  Deliverables:
  - Recurring transaction templates
  - Automated creation working
  - Proper edit/delete handling

  ---
  PHASE 6: Installment Enhancements (Week 10) 🔵

  Priority: MEDIUM - Improve existing feature

  1. Installment Group Operations
    - Create all installments at once
    - Edit entire group (description, category, etc.)
    - Delete entire group with confirmation
  2. Installment Forecasting
    - Upcoming installments view
    - Total remaining calculation
    - Paid vs pending status
  3. Early Payoff Handling
    - Mark remaining installments as paid
    - Interest adjustment support

  Deliverables:
  - Bulk installment management
  - Forecast endpoints
  - Early payoff workflows

  ---
  PHASE 7: Notifications & Reminders (Week 11) 🟣

  Priority: LOW-MEDIUM - User engagement

  1. Notification System
    - In-app notification entity
    - Notification preferences per user
    - Mark as read/unread
  2. Email Notifications
    - Bill due date reminders (3 days, 1 day before)
    - Credit card closing date alerts
    - Budget threshold alerts
    - Weekly/monthly summaries
  3. Notification Scheduler
    - Spring @Scheduled jobs
    - Configurable delivery times
    - Opt-in/opt-out per notification type

  Deliverables:
  - Notification infrastructure
  - Email integration
  - Reminder jobs running

  ---
  PHASE 8: Advanced Features (Week 12+) 🟣

  Priority: LOW - Nice-to-have enhancements

  1. File Attachments
    - Receipt/invoice upload (S3 or local)
    - Image preview
    - PDF storage
  2. Multi-Currency Support
    - Currency entity and management
    - Exchange rate API integration
    - Multi-currency transactions
  3. Smart Categorization
    - ML-based category suggestions
    - Pattern learning from user corrections
    - Confidence scoring
  4. Advanced Analytics
    - Spending patterns detection
    - Anomaly detection
    - Predictive forecasting
  5. Enhanced User Experience
    - Saved search templates
    - Custom dashboard widgets
    - Profile pictures
    - Dark mode preferences

  Deliverables:
  - Storage system operational
  - Multi-currency working
  - ML suggestions available

  ---
  RECOMMENDED TECH ADDITIONS

  - Flyway/Liquibase - Database schema migration management
  - Redis - Caching layer for reports and summaries
  - Spring Cloud AWS - S3 integration for file storage
  - Quartz Scheduler - Advanced scheduling for recurring transactions
  - Micrometer + Prometheus - Production monitoring
  - Sentry/Rollbar - Error tracking
  - SendGrid/AWS SES - Email delivery

  ---
  SUCCESS METRICS

  Phase 1: Zero security vulnerabilities, all data properly isolated
  Phase 2: 100% CRUD completeness across all entities
  Phase 3: Users can generate and export all standard reports
  Phase 4: Users can set budgets and receive alerts
  Phase 5: Recurring bills auto-created without manual entry
  Phase 6: Full installment lifecycle manageable in one flow
  Phase 7: Users receive timely notifications for important events
  Phase 8: Advanced features differentiate from competitors

  ---
  Estimated Total Timeline: 12-16 weeks for full feature parity
  MVP for Production: Complete Phases 1-3 (6 weeks)

  ---
  ## RECENTLY IMPLEMENTED FEATURES

  ### Universal Advanced Filtering for Charts, Reports & Exports ✅ NEW!
  **All dashboard, report, and export endpoints now support comprehensive filtering**

  #### Enhanced Endpoints:
  **Dashboard:**
  - `GET /v1/dashboard/summary` - Dashboard with income, expenses, top categories, upcoming bills
  - `GET /v1/dashboard/visualization/expense-trend` - Time series expense trend data
  - `GET /v1/dashboard/visualization/category-distribution` - Category pie/donut chart data

  **Reports:**
  - `GET /v1/reports/category` - Category breakdown with hierarchy rollup
  - `GET /v1/reports/user` - User expense breakdown
  - `GET /v1/reports/credit-card` - Credit card expense breakdown
  - `GET /v1/reports/month-comparison` - Month-over-month comparison

  **Exports:**
  - `GET /v1/export/csv` - Export to CSV
  - `GET /v1/export/pdf` - Export to PDF report
  - `GET /v1/export/excel` - Export to Excel/XLSX

  #### Available Filter Parameters (ALL OPTIONAL):
  | Parameter | Type | Description | Example |
  |-----------|------|-------------|---------|
  | `startDate` | LocalDate | Start date (required) | `2025-01-01` |
  | `endDate` | LocalDate | End date (required) | `2025-01-31` |
  | `categoryIds` | List<Long> | Filter by specific categories | `1,2,3` |
  | `responsibleUserIds` | List<Long> | Filter by responsible users | `5,7` |
  | `creditCardIds` | List<Long> | Filter by credit cards | `10,11` |
  | `transactionTypes` | List<Enum> | Filter by type | `INSTALLMENT,ONE_TIME` |
  | `transactionSources` | List<Enum> | Filter by source | `MANUAL,CSV_IMPORT` |
  | `minAmount` | BigDecimal | Minimum transaction amount | `100.00` |
  | `maxAmount` | BigDecimal | Maximum transaction amount | `1000.00` |

  #### Example Usage:
  ```

# Get expense trend for Food & Entertainment categories only

GET /v1/dashboard/visualization/expense-trend?startDate=2025-01-01&endDate=2025-01-31&categoryIds=1,2

# Category report for User A, excluding CSV imports

GET /v1/reports/category?startDate=2025-01-01&endDate=2025-12-31&responsibleUserIds=5&transactionSources=MANUAL

# Export transactions over $500 from specific credit card

GET /v1/export/csv?startDate=2025-01-01&endDate=2025-03-31&creditCardIds=3&minAmount=500.00

# Month comparison for installment transactions only

GET /v1/reports/month-comparison?startDate=2025-01-01&endDate=2025-12-31&transactionTypes=INSTALLMENT

  ```

  #### Key Benefits:
  - **Complete Flexibility**: Generate charts with ANY combination of filters
  - **No Client-Side Filtering**: All filtering done server-side for performance
  - **Consistent API**: Same filter parameters across all endpoints
  - **Transaction Type Filtering**: NEW - Filter by ONE_TIME, INSTALLMENT, RECURRING, PAYMENT, REFUND
  - **Transaction Source Filtering**: NEW - Filter by MANUAL, CSV_IMPORT, INVOICE_IMPORT, SYSTEM
  - **AND/OR Logic**: Multiple IDs within same parameter use OR logic; different parameters use AND logic

  #### Technical Implementation:
  - `TransactionFilter` record enhanced with `transactionTypes` and `transactionSources`
  - `TransactionSpecFactory` updated with new specifications
  - All services migrated from repository.search() to specification-based queries
  - Full Swagger documentation with parameter descriptions

  ### CSV Import Rollback (#5) ✅
  - **Endpoint**: `DELETE /v1/csv-imports/{id}/rollback`
  - **Capability**: Rollback any CSV import by soft-deleting all associated transactions
  - **Status Tracking**: Updates import status to `ROLLED_BACK`
  - **Validation**: Ensures import belongs to requesting account
  - **Unit Tests**: Complete coverage in `CsvImportServiceTest.java`

  ### Installment Group Management (#10) ✅
  - **Bulk Create**: `POST /v1/installment-groups` creates all installments in one request
  - **Full Update**: `PUT /v1/installment-groups/{id}` updates all installments (description, amount, category, credit card, responsible users)
  - **Partial Update**: `PATCH /v1/installment-groups/{id}/description` updates description only
  - **Bulk Delete**: `DELETE /v1/installment-groups/{id}` removes entire group
  - **Summary View**: `GET /v1/installment-groups/{id}` shows paid/pending breakdown
  - **Features**:
    - Sequential monthly dates automatically calculated
    - Supports 2-120 installments per group
    - Tracks paid vs pending installments
    - Total amount automatically split across installments
    - Full category, credit card, and responsible users support
    - Update entire group without recreating individual installments
  - **Unit Tests**: Comprehensive coverage in `InstallmentGroupServiceTest.java`

  ### User Preferences System (#12) ✅
  - **Endpoints**:
    - `GET /v1/users/preferences` - Retrieve user preferences
    - `PUT /v1/users/preferences` - Update preferences
    - `DELETE /v1/users/preferences` - Reset to defaults
  - **Settings Available**:
    - Theme (Light/Dark/Auto)
    - Language (EN/PT)
    - Profile picture URL
    - Notification preferences (in-app, email, budget alerts, bill reminders)
    - Weekly summary opt-in
    - Default currency (3-letter code: BRL, USD, EUR, etc.)
  - **Features**:
    - Auto-creates default preferences on first access
    - One-to-one relationship with User entity
    - Full validation with localized error messages
    - Soft delete support with audit trail
  - **Unit Tests**: Complete coverage in `UserPreferencesServiceTest.java`

  ### Database Schema Updates
  - **New Table**: `user_preferences` with unique constraint on user_id
  - **New Enums**: `Theme` (LIGHT, DARK, AUTO), `Language` (EN, PT)
  - **Repository Method**: `findByInstallmentGroupIdAndAccountId` for installment queries

  ### Message Keys Added
  ```properties
  # Installment Group
  installment.total.notNull=Total installments is required
  installment.total.min=Total installments must be at least 2
  installment.total.max=Total installments cannot exceed 120
  installment.firstDate.notNull=First installment date is required
  installment.group.notFound=Installment group {0} not found
  installment.group.accountMismatch=Installment group does not belong to this account

  # User Preferences
  resource.userPreferences.notFound=User preferences not found
  userPreferences.profilePictureUrl.size=Profile picture URL cannot exceed 500 characters
  userPreferences.currency.pattern=Currency code must be 3 uppercase letters (e.g., BRL, USD, EUR)
  ```