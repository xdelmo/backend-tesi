# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ApexFlow backend: Spring Boot REST API (Java 21, Spring Boot 4.0.1, Maven, PostgreSQL) serving an Angular 19 + PrimeNG frontend (customers, products, orders, users, customer stats). Code comments, models and domain concepts are in Italian.

## Commands

```bash
./mvnw clean install                 # build + full test suite
./mvnw spring-boot:run               # run on http://localhost:8080
./mvnw test                          # run all tests
./mvnw test -Dtest=CustomerControllerTest # run single test class
```

Requires a reachable PostgreSQL instance (local or remote).
Defaults in `application.properties`:
- `spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/apexflow_db}`
- `spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}`
- `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:admin}`
- `server.port=${PORT:8080}`
- `spring.mvc.problemdetails.enabled=true`

## Architecture & Conventions (Conforme a `springboot-patterns`)

Layered architecture under `com.apexflow`:
`controller` → `service` → `repository` → `model`

### 1. Iniezione delle Dipendenze
- **Constructor Injection esclusiva**: Campi `private final` con costruttore esplicito (o Lombok `@RequiredArgsConstructor`).
- **ANTI-PATTERN VIETATO**: Mai usare `@Autowired` sui campi privati.

### 2. Transazionalità
- I Service dichiarano `@Transactional(readOnly = true)` a livello di classe per ottimizzare le sessioni di lettura JPA.
- I metodi di mutazione (`create*`, `update*`, `delete*`) dichiarano esplicitamente `@Transactional`.

### 3. Paginazione, Ordinamento e Filtri Dinamici
- Gestiti tramite `QueryFilterParser`:
  - `_page`: 1-indexed (convertito a 0-indexed per Spring Data `PageRequest`).
  - `_limit`: dimensione pagina (default 10).
  - `_sort` e `_order`: ordinamento per campo (`asc`/`desc`).
  - `*_like`: ricerca case-insensitive substring `like lower("%val%")`.
  - Campi esatti (es. `status=Attivo`).
  - Filtri cross-entity: es. `customer.name_like` sugli ordini.
  - Ordinamento speciale: `_sort=customerStats.totalRevenue` sui clienti.
- Le risposte paginate restituiscono l'header HTTP `X-Total-Count` valorizzato con il totale degli elementi (`page.getTotalElements()`).

### 4. Codici di Stato HTTP
- `GET /api/<resource>`: `200 OK` con `X-Total-Count` header.
- `GET /api/<resource>/{id}`: `200 OK` se presente, `404 NOT FOUND` se assente.
- `POST /api/<resource>`: `201 CREATED` con payload della risorsa creata.
- `PATCH /api/<resource>/{id}`: `200 OK` con payload aggiornato, `404 NOT FOUND` se assente.
- `DELETE /api/<resource>/{id}`: `204 NO CONTENT` se eliminato con successo, `404 NOT FOUND` se non trovato.

### 5. Dominio & Relazioni
- Gli ID entità sono `String` (es. generati a 16 caratteri per `Customer`, prefissati `ORD-` per `Order`, `PROD-` per `Product`). Solo `User` e `OrderItem` usano ID numerici auto-incrementali `Long`.
- `Customer`: include `subscriptionEndDate` (`LocalDate`) e `@Transient List<CustomerStats> customerStats` per embedding (`_embed=customerStats`).
- `User`: include `@CreationTimestamp createdAt` e `@UpdateTimestamp updatedAt`.
- `Order` e `OrderItem`: associazione bidirezionale JPA (Cascade `ALL`, `orphanRemoval = true`). `OrderItem.order` è annotato con `@JsonIgnore`, `@ToString.Exclude` e `@EqualsAndHashCode.Exclude` per evitare cicli di serializzazione e ricorsione.
- Transazione creazione ordine: `OrderService.createOrder` salva l'ordine e se contiene prodotti di tipo abbonamento (*Mensile* o *Annuale*), estende atomicamente `subscriptionEndDate` del cliente, imposta lo stato ad `"Attivo"` e crea/aggiorna il record aggregato in `CustomerStats`.

### 6. Sicurezza, CORS e Middleware
- `SecurityConfig`: CSRF disabilitato, endpoint `/api/**` accessibili per compatibilità con il token frontend. CORS configurato globalmente esponendo `X-Total-Count` e `Authorization`.
- `RequestLoggingFilter`: `OncePerRequestFilter` per il tracciamento strutturato con SLF4J della latenza (`durationMs`), metodo, URI e codice HTTP di risposta.

## Deployment

Deploy su Render.com tramite `Dockerfile` (multi-stage, Eclipse Temurin 21) ad ogni push su `main`.
URL: https://backend-tesi.onrender.com/
