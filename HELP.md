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