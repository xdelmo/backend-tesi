# 🔹 ApexFlow Backend - Spring Boot API

Backend RESTful API per l'applicazione enterprise **ApexFlow**, sviluppato con **Spring Boot 4.0.1** e **Java 21**, conforme ai pattern di architettura enterprise definiti in `springboot-patterns`.
Fornisce servizi completi per la gestione di clienti, prodotti, ordini, statistiche e utenti, integrandosi nativamente con la dashboard Angular 19 (`dashboard-tesi`).

---

## 🚀 Funzionalità e Specifiche API

Tutti gli endpoint REST sono esposti sotto il prefisso radice `/api` (ad eccezione della radice di health check `/`) e supportano un sistema trasparente e standardizzato di paginazione, ordinamento e filtro.

### 📋 Convenzioni di Paginazione, Filtri e Header

Tutte le collezioni di risorse supportano parametri standard per la manipolazione dei risultati:
- **Query Params**:
  - `_page`: Numero di pagina (1-indexed per conformità PrimeNG, default `1`).
  - `_limit`: Numero di elementi per pagina (default `10`).
  - `_sort`: Nome della proprietà/campo su cui ordinare (es. `name`, `date`, `total`, `customerStats.totalRevenue`).
  - `_order`: Direzione dell'ordinamento (`asc` o `desc`, default `asc`).
  - `*_like`: Ricerca case-insensitive per sottostringa (es. `name_like=Tech`, `email_like=gmail`).
  - `customer.name_like`: Ricerca cross-entity (es. sugli ordini per filtrare in base al nome del cliente).
  - `q`: Ricerca generica full-text (ricerca combinata sui campi principali della risorsa).
  - Filtri esatti: Qualsiasi proprietà del modello (es. `status=Attivo`, `category=Cloud`).
- **Header di Risposta**:
  - `X-Total-Count`: Conteggio complessivo dei record corrispondenti ai criteri di filtro prima della paginazione.
  - `Access-Control-Expose-Headers: X-Total-Count, Authorization`

---

### 👥 Clienti (`/api/customers`)

Gestione completa del ciclo di vita anagrafico e degli abbonamenti dei clienti enterprise.

| Metodo | URI | Status Successo | Descrizione |
|---|---|---|---|
| `GET` | `/api/customers` | `200 OK` | Elenco clienti paginato e filtrabile. Supporta `_embed=customerStats` e `_sort=customerStats.totalRevenue`. |
| `GET` | `/api/customers/{id}` | `200 OK` / `404` | Dettaglio cliente con statistiche associate. |
| `POST` | `/api/customers` | `201 CREATED` | Creazione nuovo cliente (assegna ID univoco a 16 caratteri se omesso). |
| `PATCH` | `/api/customers/{id}` | `200 OK` / `404` | Aggiornamento parziale delle informazioni del cliente. |
| `DELETE` | `/api/customers/{id}` | `204 NO CONTENT` / `404` | Eliminazione fisica del record cliente. |

#### Esempio Richiesta Creazione Cliente (`POST /api/customers`)
```json
{
  "name": "Mario Rossi",
  "email": "mario@rossi.tech",
  "company": "Tech Spa",
  "status": "Attivo",
  "industry": "Software",
  "plan": "Enterprise",
  "subscriptionEndDate": "2027-01-21"
}
```

#### Esempio cURL Paginazione con Header
```bash
curl -i "http://localhost:8080/api/customers?_page=1&_limit=5&status=Attivo&_sort=name&_order=asc"
```

---

### 📦 Ordini & Transazioni (`/api/orders`)

Gestione transazionale degli acquisti, calcolo del fatturato, estensione automatica degli abbonamenti e aggiornamento statistiche.

| Metodo | URI | Status Successo | Descrizione |
|---|---|---|---|
| `GET` | `/api/orders` | `200 OK` | Elenco ordini paginato. Supporta filtri `customer.name_like`, status e date. |
| `GET` | `/api/orders/{id}` | `200 OK` / `404` | Dettaglio ordine con lista items (`OrderItem`). |
| `POST` | `/api/orders` | `201 CREATED` | Creazione ordine con logica transazionale atomica per estensione abbonamenti e statistiche. |
| `DELETE` | `/api/orders/{id}` | `204 NO CONTENT` / `404` | Eliminazione ordine e item associati (cascade orphan removal). |
| `GET` | `/api/orders/stats/revenue` | `200 OK` | Restituisce il fatturato totale complessivo per tutti gli ordini con stato `"Pagato"`. |
| `GET` | `/api/orders/stats/monthly-revenue` | `200 OK` / `400` | Restituisce un array di 12 valori numerici con i ricavi mensili per l'anno specificato (`?year=YYYY`). |

#### Esempio Creazione Ordine (`POST /api/orders`)
```json
{
  "customerId": "A1b2C3d4E5f6G7h8",
  "date": "2026-03-15T10:30:00",
  "status": "Pagato",
  "type": "Abbonamento",
  "subtotal": 500.0,
  "tax": 49.5,
  "discountAmount": 50.0,
  "total": 499.5,
  "items": [
    {
      "productId": "PROD-CL-001",
      "name": "Archiviazione Enterprise",
      "price": 500.0,
      "quantity": 1,
      "category": "Cloud"
    }
  ]
}
```

#### Logica Transazionale & Effetti Collaterali:
1. **Estensione Abbonamento**: Quando un ordine include prodotti con ricorrenza (`duration: "Mensile"` o `"Annuale"`), la scadenza `subscriptionEndDate` del cliente viene prorogata (aggiungendo mesi o anni a partire dalla data odierna o dalla data di scadenza futura già attiva) e lo stato del cliente viene impostato automaticamente su `"Attivo"`.
2. **Aggiornamento Statistiche Cliente**: Viene aggiornato atomicamente il record `CustomerStats` associato al cliente incrementando `totalOrders`, sommando il valore totale su `totalRevenue` e aggiornando `lastOrderDate`.

#### Esempi cURL Statistiche
```bash
# Fatturato complessivo ordini pagati
curl -s "http://localhost:8080/api/orders/stats/revenue"
# Risposta: {"totalRevenue": 1698.3}

# Fatturato mensile per l'anno 2026
curl -s "http://localhost:8080/api/orders/stats/monthly-revenue?year=2026"
# Risposta: [0.0, 499.5, 1198.8, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
```

---

### 🏷️ Prodotti & Servizi SaaS (`/api/products`)

Catalogo dei prodotti e servizi software/infrastrutturali offerti dalla piattaforma.

| Metodo | URI | Status Successo | Descrizione |
|---|---|---|---|
| `GET` | `/api/products` | `200 OK` | Elenco prodotti paginato e filtrabile (`category`, `status`, `duration`, `name_like`). |
| `GET` | `/api/products/{id}` | `200 OK` / `404` | Dettaglio singolo prodotto per ID (es. `PROD-CL-001`). |
| `POST` | `/api/products` | `201 CREATED` | Inserimento a catalogo di un nuovo prodotto. |
| `PATCH` | `/api/products/{id}` | `200 OK` / `404` | Aggiornamento parziale di un prodotto (prezzo, disponibilità, descrizione). |
| `DELETE` | `/api/products/{id}` | `204 NO CONTENT` / `404` | Rimozione fisica del prodotto da catalogo. |

#### Esempio Richiesta Creazione Prodotto (`POST /api/products`)
```json
{
  "id": "PROD-AI-002",
  "name": "Apex Copilot Pro",
  "description": "Assistente AI generativo per analisi predittive aziendali",
  "category": "AI",
  "price": 299.0,
  "duration": "Mensile",
  "status": "Attivo"
}
```

#### Esempio cURL Prodotti
```bash
# Ricerca prodotti attivi nella categoria AI
curl -s "http://localhost:8080/api/products?category=AI&status=Attivo"
```

---

### 👤 Utenti della Piattaforma (`/api/users`)

Gestione degli account utente e dei relativi ruoli di accesso alla dashboard.

| Metodo | URI | Status Successo | Descrizione |
|---|---|---|---|
| `GET` | `/api/users` | `200 OK` | Elenco utenti paginato con supporto filtri `role`, `email_like`, `name_like`. |
| `GET` | `/api/users/{id}` | `200 OK` / `404` | Dettaglio utente per ID numerico progressivo (`Long`). |
| `POST` | `/api/users` | `201 CREATED` | Registrazione nuovo utente con timestamp automatico `createdAt`. |
| `PATCH` | `/api/users/{id}` | `200 OK` / `404` | Aggiornamento dati profilo con aggiornamento automatico `updatedAt`. |
| `DELETE` | `/api/users/{id}` | `204 NO CONTENT` / `404` | Eliminazione utente. |

#### Esempio Richiesta Creazione Utente (`POST /api/users`)
```json
{
  "name": "Giulia Bianchi",
  "email": "giulia.bianchi@apexflow.io",
  "password": "hashed_password",
  "role": "ADMIN",
  "avatar": "assets/avatars/avatar-2.png"
}
```

---

### 📊 Statistiche Clienti (`/api/customerStats`)

Dati analitici aggregati per cliente relativi al volume d'affari complessivo e allo storico transazioni.

| Metodo | URI | Status Successo | Descrizione |
|---|---|---|---|
| `GET` | `/api/customerStats` | `200 OK` | Elenco paginato statistiche, filtrabile per `customerId`. |
| `GET` | `/api/customerStats/{id}` | `200 OK` / `404` | Dettaglio statistica per ID univoco (es. `CS-001`). |
| `POST` | `/api/customerStats` | `201 CREATED` | Inserimento manuale record statistiche. |
| `PATCH` | `/api/customerStats/{id}` | `200 OK` / `404` | Aggiornamento valori fatturato o conteggio ordini. |

---

### 🏠 System Info (`/`)

Endpoint radice per verifica rapida di stato del servizio.

- **`GET /`** -> Restituisce stato, versione dell'API e link al repository:
  ```json
  {
    "status": "UP",
    "version": "0.0.1-SNAPSHOT",
    "message": "Welcome to ApexFlow Backend API",
    "documentation": "https://github.com/xdelmo/backend-tesi"
  }
  ```

---

## ⚠️ Gestione Centralizzata delle Eccezioni (RFC 7807)

L'applicazione adotta una gestione centralizzata e controllata delle eccezioni tramite `@RestControllerAdvice` con la classe `GlobalExceptionHandler`, conforme alle specifiche **RFC 7807 (Problem Details)**:
- Abilitato nativamente via `spring.mvc.problemdetails.enabled=true`.
- Tutte le risposte di errore adottano il DTO immutabile `ApiError` (`Java record`), garantendo un payload JSON omogeneo, prevedibile e facilmente interpretabile dal frontend Angular.

### Schema del DTO `ApiError`
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descrizione dettagliata dell'errore",
  "timestamp": "2026-10-31T13:28:00.123456Z"
}
```

### Tipologie di Errori Gestite:
1. **Errori di Validazione DTO (`MethodArgumentNotValidException`)**:
   - Status: `400 BAD REQUEST`
   - Payload: `error: "Validation Error"`, `message` contenente la lista formattata dei campi non validi (`campo: messaggio, ...`).
2. **Argomenti Illegali o Non Validi (`IllegalArgumentException`)**:
   - Status: `400 BAD REQUEST`
   - Payload: `error: "Bad Request"`, `message` contenente la causa sollevata dal servizio applicativo.
3. **Eccezioni Non Previste / Fallback (`Exception`)**:
   - Status: `500 INTERNAL SERVER ERROR`
   - Tracciamento: Notifica su SLF4J tramite log strutturato `log.error("unexpected_error", ex)` con stack trace completo.
   - Payload esposto al client: `error: "Internal Server Error"`, `message: "Si è verificato un errore inatteso"` (protezione da data-leakage e dettagli architetturali interni).

---

## ⏰ Schedulazione Automatica (`SubscriptionScheduler`)

Per garantire l'allineamento automatico degli abbonamenti SaaS:
- È configurato il componente `@Scheduled` `SubscriptionScheduler` attivo con espressione cron:
  ```cron
  0 0 2 * * ?
  ```
- **Frequenza**: Ogni notte alle ore **02:00**.
- **Logica**: Interroga il database per identificare tutti i clienti con stato `"Attivo"` e `subscriptionEndDate` antecedente alla data odierna (`LocalDate.now()`). Imposta atomicamente il loro stato a `"Inattivo"` registrando le variazioni con SLF4J.

---

## 🛡️ Sicurezza, CORS e Middleware

- **CORS Globale**: Configurato in `SecurityConfig` per autorizzare le chiamate dal frontend Angular (`http://localhost:4200`), abilitando i metodi HTTP `GET, POST, PUT, PATCH, DELETE, OPTIONS`.
- **Exposed Headers**: `X-Total-Count` e `Authorization` sono espressamente esposti per consentire la lettura del conteggio totale dei record e la corretta visualizzazione della paginazione lato PrimeNG.
- **RequestLoggingFilter**: Filtro HTTP (`OncePerRequestFilter`) che registra tramite log strutturato SLF4J la metrica di latenza (`durationMs`), il metodo HTTP, l'URI e il codice HTTP di risposta per ogni singola invocazione.
- **Protezione CSRF**: Disabilitata per endpoint stateless compatibili con comunicazioni REST standard.

---

## 🗄️ Inizializzazione Idempotente Dati (`DataInitializer`)

All'avvio dell'applicazione, `DataInitializer` popola in modo idempotente il database PostgreSQL leggendo il dataset completo da `src/main/resources/data/seed-db.json` (speculare a `dashboard-tesi/db.json`):
- **Utenti (`users`)**: 2 account preconfigurati (`admin@demo.com`, `user@demo.com`).
- **Clienti (`customers`)**: 8 profili aziendali con piani di abbonamento e scadenze.
- **Statistiche Clienti (`customer_stats`)**: 5 record con ricavi aggregati, ordini totali e date ultimo ordine.
- **Prodotti (`products`)**: 8 prodotti/servizi enterprise (Cloud, Analytics, AI, Consulenza).
- **Ordini (`orders` & `order_items`)**: 7 ordini con integrità referenziale bidirezionale `OrderItem -> Order` preservata.
- **Idempotenza & Logging**: Esegue verifiche preventive del conteggio record su ciascun repository (`repo.count() == 0`) per evitare duplicazioni e registra i passi salienti tramite SLF4J (`LoggerFactory.getLogger(DataInitializer.class)`).
- **Iniezione**: Adotta la Constructor Injection pura su tutti i repository e sull'`ObjectMapper`.

---

## 🛠️ Tech Stack

- **Linguaggio:** [Java 21](https://www.java.com/)
- **Framework:** [Spring Boot 4.0.1](https://spring.io/projects/spring-boot)
- **Database:** [PostgreSQL](https://www.postgresql.org/)
- **ORM / Persistenza:** Hibernate 6 / Spring Data JPA (Criteria API & Specification)
- **Error Handling:** RFC 7807 Problem Details (`@RestControllerAdvice`)
- **Logging:** SLF4J / Logback strutturato
- **Build Tool:** Apache Maven Wrapper (`./mvnw`)
- **Containerizzazione:** Docker (Eclipse Temurin 21)

---

## 📂 Struttura del Progetto

```text
src/main/java/com/apexflow/
├── ApexFlowApplication.java   # Entry point con @EnableScheduling
├── config/                    # SecurityConfig (CORS, permitAll), RequestLoggingFilter, DataInitializer
├── controller/                # CustomerController, OrderController, ProductController, UserController, CustomerStatsController, HomeController
├── exception/                 # GlobalExceptionHandler (@RestControllerAdvice), ApiError (record DTO)
├── model/                     # Customer, Order, OrderItem, Product, User, CustomerStats
├── repository/                # JpaRepository & JpaSpecificationExecutor interfaces
├── scheduler/                 # SubscriptionScheduler (aggiornamento notturno scadenze abbonamenti)
├── service/                   # CustomerService, OrderService, ProductService, UserService, CustomerStatsService
└── util/                      # QueryFilterParser (conversione trasparente parametri HTTP in Specification e Pageable)
```

---

## ▶️ Setup & Installazione Locale

### 1. Compila ed esegui i test:
```bash
./mvnw clean test
```

### 2. Avvia il server backend:
```bash
./mvnw spring-boot:run
```
L'API REST sarà accessibile su `http://localhost:8080/api` (e `http://localhost:8080/` per l'health check).

### 3. Collega il Frontend Angular (`dashboard-tesi`):
Dalla directory del frontend, avvia la dashboard configurata per comunicare con il backend Spring Boot su porta 8080:
```bash
cd ../dashboard-tesi
npm run start:springboot
```
La dashboard web sarà disponibile su `http://localhost:4200` interfacciata direttamente con l'API Spring Boot.

---

**Autore:** Emanuele Del Monte  
**Progetto:** Advanced Web Development / Tesi ApexFlow SaaS Enterprise Dashboard
