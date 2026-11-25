# xPository

A lightweight metadata management system (MMS) with REST API-first architecture for organizing and managing metadata through collections, elements, and custom element types.

## Overview

xPository provides flexible metadata management built on Spring Boot with a REST API as the primary interface. The system includes:

- **REST API Backend:** Spring Boot application exposing all CRUD operations
- **Admin Frontend:** Angular SPA for system administration *TODO*
- **SQLite Storage:** Lightweight, file-based database for metadata persistence
- **Access Control:** Permission-based security with inheritance
- **Versioning:** Track element changes with major and minor versions

## Prerequisites

- **Java 25** (required)
- **Maven 3.9+** (required)

## Quick Start

### Build the Project

```bash
# Clone the repository
git clone <repository-url>
cd xpository

# Build with Maven
mvn clean install
```

### Run the Application

```bash
# Start with development profile (default)
mvn spring-boot:run

# Start with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Application will be available at:
# http://localhost:8080
```

### Environment Profiles

xPository supports three profiles for different environments:

| Profile | Database | Schema Mode | SQL Logging | Use Case |
|---------|----------|-------------|-------------|----------|
| **dev** | `xpository-dev.db` | auto-update | enabled | Local development |
| **test** | in-memory | create-drop | enabled | Automated testing |
| **prod** | configurable | validate | disabled | Production deployment |

**Production configuration** via environment variables:
```bash
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_PATH=/var/lib/xpository/xpository.db
export INDEX_PATH=/var/lib/xpository/indexes
export CONFIG_PATH=/etc/xpository
java -jar target/xpository.jar
```

See [.env.example](.env.example) for all available environment variables.

### Run Tests

```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

**Current Test Coverage:**
- **372 tests** (372 passing, 1 skipped)
- **Overall**: 70%+ line coverage, 60%+ branch coverage
- **Service Layer**: 85%+ line coverage, 70%+ branch coverage
- **Controller Layer**: Integration tested via TestRestTemplate with real database
-- Updating to support RestTestClient
- **Breakdown**: 43 service tests, 22 trait controller tests, 66 total trait tests, 100+ others

## Development

### Code Formatting

This project uses Google Java Format via Spotless. Format code before committing:

```bash
mvn spotless:apply
```

### Project Structure

```
xpository/
├── src/
│   ├── main/
│   │   ├── java/net/ottleys/xpository/  # Application source code
│   │   └── resources/                    # Configuration files
│   └── test/
│       └── java/net/ottleys/xpository/  # Test suites
└── pom.xml      # Maven build configuration
```

### Documentation

- TODO

## Technology Stack

- **Backend:** Java 25, Spring Boot 4.2.0
- **Database:** SQLite (via JDBC)
- **Security:** Spring Security
- **Frontend:** Angular (separate build)
- **Testing:** JUnit 5, Spring Test, Mockito


## License

Apache License, Version 2.0

## Contact

Jared Ottley - jared@ottleys.net