# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ApexFlow backend: Spring Boot REST API (Java 21, Maven) serving an Angular frontend (customers, products, orders, users, customer stats). Code comments and seed data are in Italian.

## Commands

```bash
./mvnw clean install                 # build + tests
./mvnw spring-boot:run               # run on http://localhost:8080
./mvnw clean package -DskipTests     # what the Dockerfile runs
./mvnw test -Dtest=ApexFlowApplicationTests#contextLoads   # single test
```

The app (and the only test, a `@SpringBootTest` context load) needs a reachable PostgreSQL. Defaults in `application.properties`: `jdbc:postgresql://localhost:5432/apexflow_db`, user `postgres`, password `admin`; override with `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`, port with `PORT`.

## Architecture

Flat three-layer layout under `com.apexflow`: `controller` → `repository` (plain `JpaRepository` interfaces) → `model` (Lombok `@Data` JPA entities). There is **no service layer and no DTOs**: controllers inject repositories directly and serialize entities as JSON.

Conventions to follow when adding endpoints:
- Each controller has `@RequestMapping("/api/<resource>")` and `@CrossOrigin(origins = "*")`.
- Not-found returns `null` (HTTP 200 with empty body) via `findById(id).orElse(null)`, not 404.
- Updates use `PATCH` with partial-merge semantics: copy each non-null field from the request body onto the loaded entity (see `CustomerController.updateCustomer`).
- Most entity IDs are client-supplied `String`s (e.g. `PROD-CL-001`, `ORD-719`); only `User` and `OrderItem` use generated `Long` IDs.
- Relations are by ID string, not JPA associations (`Order.customerId`, `CustomerStats.customerId`, `OrderItem.productId`). The only real association is `Order` 1→N `OrderItem` (cascade ALL, orphanRemoval); `OrderItem.order` is `@JsonIgnore` to avoid recursion.

Config:
- `SecurityConfig`: Spring Security is on the classpath but configured to `permitAll()` with CSRF disabled — no auth is enforced. User passwords are stored in plain text.
- `DataInitializer`: `CommandLineRunner` that seeds each table only if empty. Schema is managed by `spring.jpa.hibernate.ddl-auto=update` (no migrations).

## Deployment

Render.com builds the `Dockerfile` (multi-stage, temurin 21) on every push to `main`, backed by a Render-managed PostgreSQL. Live: https://backend-tesi.onrender.com/
