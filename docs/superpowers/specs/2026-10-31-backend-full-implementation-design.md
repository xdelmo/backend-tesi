# Design Document: Implementazione Completa Backend ApexFlow per Frontend Angular

- **Data**: 2026-10-31
- **Stato**: Approvato (Aggiornato con standard `springboot-patterns`)
- **Progetto**: ApexFlow SaaS Enterprise Dashboard (Backend Spring Boot & Frontend Angular)
- **Autore**: AI Pair Programmer & Emanuele Del Monte

---

## 1. Obiettivo e Contesto

L'obiettivo è completare l'intero backend Spring Boot (`backend-tesi`) per renderlo il server di produzione pienamente compatibile con il frontend Angular 19 (`dashboard-tesi`). Attualmente il frontend è stato sviluppato basandosi su un mock server (`server.js` + `json-server`).

Il backend adotta un **approccio ibrido evolutivo** aderente ai pattern architetturali descritti in `springboot-patterns`:
1. **Compatibilità al 100% con il contratto HTTP del frontend**:
   - Header di paginazione: `X-Total-Count` e relativo header CORS `Access-Control-Expose-Headers`.
   - Parametri query: `_page` (1-indexed), `_limit`, `_sort`, `_order`, filtri parziali `*_like`, filtri esatti, `q` (ricerca full text), `customer.name_like`, `_embed`.
   - Endpoint statistici: `/api/orders/stats/revenue` e `/api/orders/stats/monthly-revenue?year=YYYY`.
   - Autenticazione attuale compatibile: `GET /api/users?email=...&password=...`.
2. **Standard e Best Practice Spring Boot (`springboot-patterns`)**:
   - **Constructor Injection**: eliminazione completa di `@Autowired` sui campi a favore di campi `private final` iniettati via costruttore (o `@RequiredArgsConstructor`).
   - **Transazionalità granulare**: `@Transactional(readOnly = true)` sui metodi di query e lettura; `@Transactional` specifico sulle mutazioni dati (`create`, `update`, `delete`).
   - **Codici di stato REST canonici**: `201 CREATED` per `POST`, `204 NO CONTENT` per `DELETE`, `200 OK` per `GET` e `PATCH`.
   - **Structured Logging (SLF4J)**: rimozione di qualsiasi `System.out.println` a favore di logger SLF4J parametrizzati (`log.info(...)`, `log.error(...)`).
   - **Standard RFC 7807 Problem Details**: abilitato via `spring.mvc.problemdetails.enabled=true`.
   - **Request Logging Filter**: tracciamento di durata (ms), metodo, URI e stato HTTP di ogni richiesta.
3. **Centralizzazione nel backend della logica transazionale di business**:
   - Calcolo ed estensione della data di scadenza abbonamento cliente (`subscriptionEndDate`) all'acquisto di prodotti ricorrenti.
   - Aggiornamento contestuale delle statistiche cliente (`CustomerStats`).
   - Task periodico schedulato (`@Scheduled`) per marcare come "Scaduto" gli abbonamenti oltre il termine.
4. **Allineamento iniziale automatico con il dataset completo definito in `db.json` tramite `DataInitializer`**.

---

## 2. Architettura del Sistema

```
                      +-----------------------------------+
                      |   Frontend Angular 19 (PrimeNG)   |
                      +-----------------+-----------------+
                                        | HTTP (CORS, X-Total-Count)
                                        v
                      +-----------------------------------+
                      |       Security & CORS Filter      |
                      +-----------------+-----------------+
                                        |
                                        v
                      +-----------------------------------+
                      |       RequestLoggingFilter        |
                      |   (Audit latenze e URI richieste) |
                      +-----------------+-----------------+
                                        |
                                        v
                      +-----------------------------------+
                      |          REST Controllers         |
                      | (Customer, Order, Product, User)  |
                      |   [Constructor Injection, DTOs]   |
                      +-----------------+-----------------+
                                        |
                                        v
                      +-----------------------------------+
                      |           Service Layer           |
                      | [@Transactional / readOnly=true]  |
                      +--------+-----------------+--------+
                               |                 |
                               v                 v
            +---------------------+   +---------------------+
            |  QueryFilterParser  |   | SubscriptionSchedule|
            | (Criteria API/Specs)|   |   (@Scheduled Cron) |
            +----------+----------+   +---------------------+
                       |
                       v
            +---------------------+
            |   JPA Repositories  |
            +----------+----------+
                       |
                       v
            +---------------------+
            | PostgreSQL Database |
            +---------------------+
```

### 2.1 Componenti Chiave
1. **`SecurityConfig` & CORS**:
   - Disabilitazione CSRF (API stateless).
   - Abilitazione accesso endpoints `/api/**`.
   - Configurazione CORS con `Access-Control-Expose-Headers: X-Total-Count`.
2. **`RequestLoggingFilter`**:
   - `OncePerRequestFilter` per registrare durata, metodo HTTP, URI e status della risposta.
3. **`QueryFilterParser` (Specification Builder)**:
   - Estrae i meta-parametri di sistema (`_page`, `_limit`, `_sort`, `_order`).
   - Converte i parametri dinamici in `Specification<T>` per Spring Data JPA Criteria API (`_like`, esatti, `q`, join).
4. **Service Layer**:
   - Gestione transazionale differenziata (`readOnly = true` vs mutazioni).
   - Iniezione delle dipendenze via costruttore.
   - Logging strutturato SLF4J.
5. **Scheduler & Cron**:
   - `SubscriptionScheduler`: controllo periodico clienti attivi con data scaduta.

---

## 3. Modello Dati e Allineamento Entità

### 3.1 `Customer`
- `id`: String (16 caratteri alfanumerici)
- `name`: String
- `email`: String
- `company`: String
- `status`: String (`Attivo`, `In Attesa`, `Inattivo`, `Scaduto`, `Sospeso`)
- `industry`: String (`Software`, `Professional Services`, `Creative`, `Retail`, `Logistics`, `Cybersecurity`, `Other`)
- `plan`: String (`Basic`, `Professional`, `Enterprise`)
- `subscriptionEndDate`: `LocalDate` (formato ISO `YYYY-MM-DD`)
- `customerStats`: `@Transient` lista di `CustomerStats` per serializzazione embedded e ordinamento su `customerStats.totalRevenue`.

### 3.2 `Product`
- `id`: String (`PROD-XX-000`)
- `name`: String
- `description`: String
- `category`: String (`Cloud`, `Analytics`, `AI`, `Consulenza`)
- `price`: Double
- `duration`: String (`Mensile`, `Annuale`, `Una Tantum`)
- `status`: String (`Attivo`, `Bozza`, `Disattivato`)

### 3.3 `Order` e `OrderItem`
- `id`: String (`ORD-XXXX`)
- `customerId`: String
- `date`: `LocalDateTime`
- `status`: String (`Pagato`, `In Attesa`, `Fallito`)
- `type`: String (`Abbonamento`, `Una Tantum`)
- `subtotal`: Double
- `tax`: Double
- `discountAmount`: Double
- `total`: Double
- `items`: `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)` lista di `OrderItem`.
  - `OrderItem`:
    - `id`: Long (generato auto)
    - `productId`: String
    - `name`: String
    - `price`: Double
    - `quantity`: Integer
    - `category`: String
    - `order`: `@ManyToOne` (annotato `@JsonIgnore`)

### 3.4 `CustomerStats`
- `id`: String
- `customerId`: String
- `totalRevenue`: Double
- `totalOrders`: Integer
- `lastOrderDate`: `LocalDateTime`

### 3.5 `User`
- `id`: Long (auto-increment)
- `email`: String
- `password`: String
- `name`: String
- `role`: String (`admin`, `user`)
- `avatar`: String
- `createdAt`: `LocalDateTime` (`@CreationTimestamp`)
- `updatedAt`: `LocalDateTime` (`@UpdateTimestamp`)

---

## 4. Dettaglio Endpoint REST

Tutti gli endpoint applicano i corretti codici di stato REST e constructor injection:

| Modulo | Metodo | Endpoint | Status Code | Descrizione |
|---|---|---|---|---|
| **Customers** | `GET` | `/api/customers` | `200 OK` | Lista paginata/filtrata (`_sort=customerStats.totalRevenue`, `_embed=customerStats`, `X-Total-Count`) |
| | `GET` | `/api/customers/{id}` | `200 OK` / `404` | Dettaglio cliente |
| | `POST` | `/api/customers` | `201 CREATED` | Creazione cliente |
| | `PATCH` | `/api/customers/{id}` | `200 OK` / `404` | Aggiornamento parziale / Soft delete (`status: "Inattivo"`) |
| | `DELETE` | `/api/customers/{id}` | `204 NO CONTENT` / `404` | Rimozione |
| **Orders** | `GET` | `/api/orders` | `200 OK` | Lista ordini (`customer.name_like`, status, date) |
| | `GET` | `/api/orders/{id}` | `200 OK` / `404` | Dettaglio ordine con `items` |
| | `POST` | `/api/orders` | `201 CREATED` | Creazione ordine (estensione abbonamento e stats atomiche) |
| | `DELETE` | `/api/orders/{id}` | `204 NO CONTENT` / `404` | Eliminazione ordine |
| | `GET` | `/api/orders/stats/revenue` | `200 OK` | `{ "totalRevenue": number }` |
| | `GET` | `/api/orders/stats/monthly-revenue?year=YYYY` | `200 OK` | `[12 float]` fatturato mensile |
| **Products** | `GET` | `/api/products` | `200 OK` | Lista prodotti paginata/filtrata |
| | `GET` | `/api/products/{id}` | `200 OK` / `404` | Dettaglio prodotto |
| | `POST` | `/api/products` | `201 CREATED` | Creazione prodotto |
| | `PATCH` | `/api/products/{id}` | `200 OK` / `404` | Aggiornamento parziale / Soft delete |
| | `DELETE` | `/api/products/{id}` | `204 NO CONTENT` / `404` | Eliminazione |
| **Users** | `GET` | `/api/users` | `200 OK` | Lista utenti (`?email=...&password=...` per login) |
| | `GET` | `/api/users/{id}` | `200 OK` / `404` | Dettaglio utente |
| | `POST` | `/api/users` | `201 CREATED` | Creazione utente |
| | `PATCH` | `/api/users/{id}` | `200 OK` / `404` | Modifica utente |
| | `DELETE` | `/api/users/{id}` | `204 NO CONTENT` / `404` | Eliminazione utente |
| **CustomerStats** | `GET` | `/api/customerStats` | `200 OK` | Lista o filtro `?customerId=...` |
| | `GET` | `/api/customerStats/{id}` | `200 OK` / `404` | Dettaglio statistiche |
| | `POST` | `/api/customerStats` | `201 CREATED` | Creazione statistiche |
| | `PATCH` | `/api/customerStats/{id}` | `200 OK` / `404` | Aggiornamento statistiche |

---

## 5. Scheduler & Job Automatici

- **Classe**: `SubscriptionScheduler`
- **Pattern**: Metodi idempotenti, esecuzione tracciata con SLF4J, transazione `@Transactional`.
- **Esecuzione**: Avvio applicazione (`@EventListener(ApplicationReadyEvent.class)`) + cron giornaliero alle 01:00 AM (`@Scheduled(cron = "0 0 1 * * ?")`).

---

## 6. Popolamento Dati Iniziali (Data Seeding)

- **Risorsa**: `src/main/resources/data/seed-db.json` (copia speculare di `dashboard-tesi/db.json`).
- **Pattern**: Iniezione da costruttore dei repository, logging SLF4J, controllo di idempotenza all'avvio.

---

## 7. Strategia di Verifica e Test

1. **Automated Unit & Integration Tests**:
   - `SecurityConfigTest`: verifica CORS ed esposizione `X-Total-Count`.
   - `QueryFilterParserTest`: test conversioni paginazione e filtri dinamici.
   - Test di integrazione MockMvc su ciascun Controller con verifica status `200`, `201`, `204`.
   - `OrderServiceTest`: verifica atomicità creazione ordine + estensione abbonamento + aggiornamento stats.
   - `SubscriptionSchedulerTest`: verifica aggiornamento clienti scaduti.
2. **End-to-End Verification**:
   - Avvio del backend Spring Boot.
   - Connessione con il frontend Angular (`npm run start:springboot`).
   - Verifica tabelle lazy-load, grafici e login.
