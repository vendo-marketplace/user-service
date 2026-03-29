# User Service

The **User Service** is responsible for managing and persisting user data in the Vendo platform.

It acts as a core data service that stores user information in the database and provides it to other services. In particular, the Auth Service relies on this service for user validation, authentication flows, and retrieving user profiles.

---

# Tech Stack

* Java 17
* Spring Boot
* JWT
* Docker
* Eureka
* Zipkin
* Micrometer
* MapStruct
* Lombok
* Maven
* JUnit 5
* Mongo
* Mockito

---

# Architecture

The service strictly follows **Hexagonal Architecture (Ports and Adapters)** to isolate the core security and authentication logic from external frameworks, databases, and message brokers.

## Layers

**domain**
Contains the core business rules and models.

**application**
Contains the application use cases and orchestration logic.

**port**
Defines interfaces used to communicate with the outside world.

**adapter**
Implementations of external integrations.
* **adapter.in**: Entry points.
* **adapter.out**: Outgoing calls.

---

# Project Structure

```
src/main/java/com/vendo/user_service
├── adapter
│   ├── in
│   │   └── web
│   └── out
│       └── persistence
├── application
│   └── command
├── domain
│   └── user
└── port
    └── user
```

---

# Prerequisites

Before running this service, ensure the required infrastructure and core services are up.

## Dependencies

This service depends on:

- **Config Server** – provides externalized configuration
- **Service Registry** (Eureka) – for service discovery 
- **Database** – for storing user data (e.g., Mongo)

---

# Running the Service

---

## 1. Clone and run Config Server

```
git clone https://github.com/vendo-marketplace/config-server
cd config-server
mvn spring-boot:run
```


---

## 2. Clone and run Service Registry

```
git clone https://github.com/vendo-marketplace/registry-service
cd registry-service
mvn spring-boot:run
```


# Running the Service

---

## 3. Run application

Or build and run:

```
mvn clean package
java -jar target/auth-service.jar
```

---

# Environment Variables

| Variable          | Description       | Default   |
|-------------------|-------------------|-----------|
| CONFIG_SERVER_URL | Config server url | 8010      |

---

# Running Tests

Run all tests

```
mvn test
```

Run integration tests

```
mvn verify
```

---

# Code Style

The project follows standard **Java code conventions**.

Key principles:

* Clean Architecture
* SOLID principles
* Immutable DTOs
* Constructor injection
* Clear separation between layers

---

# Contributing

1. Create feature branch
2. Write tests
3. Ensure tests pass
4. Create pull request

