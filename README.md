# 🔹 ApexFlow Backend - Spring Boot API

Backend RESTful API per l'applicazione enterprise **ApexFlow**, sviluppato con **Spring Boot 4.0.1** e **Java 21**, conforme ai pattern di architettura definiti in `springboot-patterns`.
Fornisce servizi completi per la gestione di clienti, prodotti, ordini, statistiche e utenti, integrandosi nativamente con la dashboard Angular 19 (`dashboard-tesi`).

---

## 🚀 Funzionalità e Specifiche API

Tutti gli endpoint REST sono esposti sotto il prefisso radice `/api` e supportano la convenzione di paginazione, ordinamento e filtro trasparente.

### 📋 Convenzioni di Paginazione e Header
- **Query Params**:
  - `_page`: Numero di pagina (1-indexed, default `1`).
  - `_limit`: Numero di elementi per pagina (default `10`).
  - `_sort`: Nome del campo su cui ordinare.
  - `_order`: Direzione (`asc` o `desc`, default `asc`).
  - `*_like`: Ricerca case-insensitive per sottostringa (es. `name_like=Tech`).
  - `q`: Ricerca generica full-text.
- **Header di Risposta**:
  - `X-Total-Count`: Conteggio totale dei record corrispondenti ai filtri applicati.
  - `Access-Control-Expose-Headers: X-Total-Count, Authorization`

---

### 👥 Clienti (`/api/customers`)

Gestione completa del ciclo di vita anagrafico e degli abbonamenti dei clienti.

| Metodo | URI | Status Successo | Descrizione |
|---|---|---|---|
| `GET` | `/api/customers` | `200 OK` | Elenco clienti paginato e filtrabile. Supporta `_embed=customerStats` e `_sort=customerStats.totalRevenue`. |
| `GET` | `/api/customers/{id}` | `200 OK` / `404` | Dettaglio cliente con statistiche associate. |
| `POST` | `/api/customers` | `201 CREATED` | Creazione nuovo cliente (assegna ID univoco a 16 caratteri se omesso). |
| `PATCH` | `/api/customers/{id}` | `200 OK` / `404` | Aggiornamento parziale / soft-delete (`status: "Inattivo"`). |
| `DELETE` | `/api/customers/{id}` | `204 NO CONTENT` / `404` | Eliminazione fisica del cliente. |

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

Gestione delle transazioni commerciali, calcolo fatturato e rinnovo automatico abbonamenti.

| Metodo | URI | Status Successo | Descrizione |
|---|---|---|---|
| `GET` | `/api/orders` | `200 OK` | Elenco ordini paginato. Supporta filtro `customer.name_like`, status e date. |
| `GET` | `/api/orders/{id}` | `200 OK` / `404` | Dettaglio ordine completo di lista items. |
| `POST` | `/api/orders` | `201 CREATED` | Creazione ordine con logica transazionale atomica per estensione abbonamenti e aggiornamento statistiche. |
| `DELETE` | `/api/orders/{id}` | `204 NO CONTENT` / `404` | Eliminazione ordine. |
| `GET` | `/api/orders/stats/revenue` | `200 OK` | Restituisce il fatturato complessivo per tutti gli ordini con stato `"Pagato"`. |
| `GET` | `/api/orders/stats/monthly-revenue` | `200 OK` / `400` | Restituisce un array di 12 elementi con i ricavi mese per mese per l'anno specificato (`?year=YYYY`). |

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

#### Effetti Collaterali della Creazione Ordine:
1. **Estensione Abbonamento**: Se l'ordine include prodotti ricorrenti (*Mensile* o *Annuale*), la data `subscriptionEndDate` del cliente viene incrementata della durata acquistata a partire da oggi (o dalla scadenza futura preesistente) e lo stato del cliente viene impostato su `"Attivo"`.
2. **Aggiornamento Statistiche**: Il record `CustomerStats` del cliente viene incrementato (`totalOrders += 1`, `totalRevenue += total`, `lastOrderDate = date`).

#### Esempi cURL Statistiche
```bash
# Fatturato complessivo ordini pagati
curl -s "http://localhost:8080/api/orders/stats/revenue"
# Risposta: {"totalRevenue": 1698.3}

# Fatturato mensile anno 2026
curl -s "http://localhost:8080/api/orders/stats/monthly-revenue?year=2026"
# Risposta: [0.0, 499.5, 1198.8, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
```

---

### 🛡 Sicurezza, CORS e Middleware

- **CORS Globale**: Autorizza l'origine del frontend Angular (`http://localhost:4200`) e abilita i metodi `GET, POST, PUT, PATCH, DELETE, OPTIONS`.
- **Exposed Headers**: `X-Total-Count` e `Authorization` sono espressamente esposti per consentire la lettura dei metadati da parte di PrimeNG.
- **RequestLoggingFilter**: Registra tramite SLF4J ogni richiesta in ingresso, monitorando latenza (`durationMs`), metodo HTTP, URI e status della risposta.
- **RFC 7807 Problem Details**: Abilitato tramite `spring.mvc.problemdetails.enabled=true` per risposte di errore standardizzate.

---

### 🗄 Inizializzazione Automatica Dati (`DataInitializer`)

All'avvio dell'applicazione, `DataInitializer` popola in modo idempotente il database PostgreSQL leggendo il dataset completo da `src/main/resources/data/seed-db.json` (speculare a `dashboard-tesi/db.json`):
- **Utenti (`users`)**: 2 account preconfigurati (`admin@demo.com`, `user@demo.com`).
- **Clienti (`customers`)**: 8 profili aziendali con piani di abbonamento e scadenze.
- **Statistiche Clienti (`customer_stats`)**: 5 record con ricavi aggregati, ordini totali e date ultimo ordine.
- **Prodotti (`products`)**: 8 prodotti/servizi enterprise (Cloud, Analytics, AI, Consulenza).
- **Ordini (`orders` & `order_items`)**: 7 ordini con relazione bidirezionale `OrderItem -> Order` preservata.
- **Idempotenza & Logging**: Esegue controlli preventivi sul conteggio delle tabelle per evitare duplicazioni e notifica tutte le operazioni tramite SLF4J (`LoggerFactory.getLogger(DataInitializer.class)`).
- **Iniezione**: Adotta la Constructor Injection pura su tutti i repository e l'`ObjectMapper`.

---

## 🛠 Tech Stack

- **Linguaggio:** [Java 21](https://www.java.com/)
- **Framework:** [Spring Boot 4.0.1](https://spring.io/projects/spring-boot)
- **Database:** PostgreSQL
- **ORM / Persistenza:** Hibernate 6 / Spring Data JPA (Criteria API)
- **Logging:** SLF4J / Logback strutturato
- **Build Tool:** Maven Wrapper (`./mvnw`)
- **Containerizzazione:** Docker (temurin-21)

---

## 📂 Struttura del Progetto

```text
src/main/java/com/apexflow/
├── config/         # SecurityConfig (CORS, permitAll), RequestLoggingFilter, DataInitializer
├── controller/     # CustomerController, OrderController, ProductController, UserController, CustomerStatsController
├── model/          # Customer, Order, OrderItem, Product, User, CustomerStats
├── repository/     # Repositories con JpaRepository & JpaSpecificationExecutor
├── service/        # CustomerService, OrderService (logiche transazionali, calcolo revenue)
└── util/           # QueryFilterParser (conversione dinamica query params in Pageable/Specification)
```

---

## ▶️ Setup & Installazione Locale

1. **Compila ed esegui i test:**
   ```bash
   ./mvnw clean test
   ```
2. **Avvia il server backend:**
   ```bash
   ./mvnw spring-boot:run
   ```
   L'API sarà disponibile su `http://localhost:8080/api`.

3. **Collega il Frontend Angular (`dashboard-tesi`):**
   ```bash
   cd ../dashboard-tesi
   npm run start:springboot
   ```
   La dashboard su `http://localhost:4200` utilizzerà direttamente il backend Spring Boot su porta `8080`.

---

**Autore:** Emanuele Del Monte  
**Progetto:** Advanced Web Development / Tesi ApexFlow SaaS Enterprise Dashboard
