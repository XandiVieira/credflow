# CredFlow

CredFlow is a Spring Boot-based backend service for managing financial transactions, accounts, and description mappings.  
It provides a secure REST API with JWT authentication, PostgreSQL persistence, and environment-specific configurations for development, testing, and production.

## Features

- **User authentication & authorization** using JWT
- **Account & transaction management** with mapping normalization
- **PostgreSQL** database integration with JPA/Hibernate
- **Profile-based configuration** (`local`, `dev`, `prod`)
- **Graceful shutdown** and GZIP compression enabled
- **Dockerized** for easy deployment
- **Ready for Render.com** deployment

## Requirements

- Java 17+
- Maven 3.9+
- PostgreSQL 14+
- Docker (optional, for containerized builds)

## Running Locally

### 1. Clone the repository
```bash
git clone https://github.com/XandiVieira/credflow.git
cd credflow