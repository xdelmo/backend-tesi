# Design Document: Implementazione Completa Backend ApexFlow per Frontend Angular

- **Data**: 2026-10-31
- **Stato**: Approvato (Pronto per Piano di Implementazione)
- **Progetto**: ApexFlow SaaS Enterprise Dashboard (Backend Spring Boot & Frontend Angular)
- **Autore**: AI Pair Programmer & Emanuele Del Monte

---

## 1. Obiettivo e Contesto

L'obiettivo è completare l'intero backend Spring Boot (`backend-tesi`) per renderlo il server di produzione pienamente compatibile con il frontend Angular 19 (`dashboard-tesi`). Attualmente il frontend è stato sviluppato basandosi su un mock server (`server.js` + `json-server`).

Il backend adotterà un **approccio ibrido evolutivo**:
1. Garantire compatibilità al 100% con il contratto HTTP atteso dal frontend:
   - Header di paginazione: `X-Total-Count` e relativo header CORS `Access-Control-Expose-Headers`.
   - Parametri query: `_page` (1-indexed), `_limit`, `_sort`, `_order`, filtri parziali `*_like`, filtri esatti, `q` (ricerca full text), `customer.name_like`, `_embed`.
   - Endpoint statistici: `/api/orders/stats/revenue` e `/api/orders/stats/monthly-revenue?year=YYYY`.
   - Autenticazione attuale compatibile: `GET /api/users?email=...&password=...`.
2. Architettura a livelli modulare (Controller, Service, Repository, Specifications, DTO, Exception Handler) per supportare future evoluzioni REST canoniche (es. autenticazione JWT rigorosa, Spring Pageable standard).
3. Centralizzazione nel backend della logica transazionale di business:
   - Calcolo ed estensione della data di scadenza abbonamento cliente (`subscriptionEndDate`) all'acquisto di prodotti ricorrenti.
   - Aggiornamento contestuale delle statistiche cliente (`CustomerStats`).
   - Task periodico schedulato (`@Scheduled`) per marcare come "Scaduto" gli abbonamenti oltre il termine.
4. Allineamento iniziale automatico con il dataset completo definito in `db.json` tramite `DataInitializer`.

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
                      |          REST Controllers         |
                      | (Customer, Order, Product, User)  |
                      +-----------------+-----------------+
                                        |
                                        v
                      +-----------------------------------+
                      |           Service Layer           |
                      |  (@Transactional, Business Logic) |
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
2. **`QueryFilterParser` (Specification Builder)**:
   - Estrae i meta-parametri di sistema (`_page`, `_limit`, `_sort`, `_order`).
   - Converte i parametri dinamici in `Specification<T>` per Spring Data JPA Criteria API:
     - `_like`: `cb.like(cb.lower(...), "%" + val.toLowerCase() + "%")`.
     - esatti: `cb.equal(...)`.
     - `q`: ricerca cross-field in `OR`.
     - filtri di join: es. `customer.name_like` su entità `Order` tramite join su `Customer`.
3. **Service Layer**:
   - `CustomerService`: CRUD, filtri, calcolo statistiche clienti e gestione soft-delete.
   - `OrderService`: gestione creazione ordini, ricalcolo `subscriptionEndDate` e `CustomerStats` in transazione atomica `@Transactional`.
   - `ProductService`: CRUD prodotti con paginazione e filtri, soft-delete.
   - `UserService`: gestione profili e compatibilità login.
   - `CustomerStatsService`: aggregazioni e query per cliente.
   - `SubscriptionScheduler`: controllo periodico clienti attivi con data scaduta.

---

## 3. Modello Dati e Allineamento Entità

### 3.1 `Customer`
- `id`: String (16 caratteri random o alfanumerico)
- `name`: String
- `email`: String
- `company`: String
- `status`: String (`Attivo`, `In Attesa`, `Inattivo`, `Scaduto`, `Sospeso`)
- `industry`: String (`Software`, `Professional Services`, `Creative`, `Retail`, `Logistics`, `Cybersecurity`, `Other`)
- `plan`: String (`Basic`, `Professional`, `Enterprise`)
- `subscriptionEndDate`: `LocalDate` (formato ISO `YYYY-MM-DD`)
- `customerStats`: `@OneToMany` o `@Transient` / join DTO per esporre `customerStats` embedded e consentire l'ordinamento per `customerStats.totalRevenue`.

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
    - `order`: `@ManyToOne` (annotato `@JsonIgnore` per evitare cicli di serializzazione)

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
- `createdAt`: `LocalDateTime`
- `updatedAt`: `LocalDateTime`

---

## 4. Dettaglio Endpoint REST

### 4.1 Customers (`/api/customers`)
- `GET /api/customers`:
  - Query params: `_page`, `_limit`, `_sort`, `_order`, `name_like`, `email_like`, `company_like`, `status`, `industry`, `plan`, `_embed=customerStats`.
  - Supporta ordinamento speciale: `_sort=customerStats.totalRevenue`.
  - Headers: `X-Total-Count: <total>`
- `GET /api/customers/{id}`: Dettaglio cliente.
- `POST /api/customers`: Creazione cliente (se ID non fornito, generato a 16 caratteri alfanumerici).
- `PATCH /api/customers/{id}`: Aggiornamento parziale (incluso soft delete con `{ "status": "Inattivo" }`).
- `DELETE /api/customers/{id}`: Rimozione.

### 4.2 Orders (`/api/orders`)
- `GET /api/orders`:
  - Query params: `_page`, `_limit`, `_sort`, `_order`, `customer.name_like`, `status`, `type`.
  - Headers: `X-Total-Count: <total>`
- `GET /api/orders/{id}`: Dettaglio ordine con `items`.
- `POST /api/orders`:
  - Body: JSON dell'ordine con array `items`.
  - Logica transazionale:
    1. Associa ciascun item all'ordine e salva l'ordine.
    2. Verifica presenza di prodotti in abbonamento (`Mensile` o `Annuale`).
    3. Estende `subscriptionEndDate` del cliente associato (`quantity * 1 mese` o `quantity * 1 anno`) partendo dalla data corrente o dalla data di scadenza futura se ancora valida. Imposta cliente a `status = 'Attivo'`.
    4. Aggiorna le statistiche in `CustomerStats` (creandole se non presenti): incrementa `totalOrders`, somma `totalRevenue`, aggiorna `lastOrderDate`.
- `DELETE /api/orders/{id}`: Eliminazione ordine.
- `GET /api/orders/stats/revenue`:
  - Risposta: `{ "totalRevenue": <somma ordini con status == 'Pagato'> }`.
- `GET /api/orders/stats/monthly-revenue?year={year}`:
  - Risposta: Array di 12 numeri con il fatturato degli ordini `Pagato` per ciascun mese (0..11) dell'anno specificato.

### 4.3 Products (`/api/products`)
- `GET /api/products`: Lista paginata e filtrata con header `X-Total-Count`.
- `GET /api/products/{id}`: Dettaglio prodotto.
- `POST /api/products`: Creazione prodotto.
- `PATCH /api/products/{id}`: Aggiornamento parziale (es. disattivazione soft delete con `status = "Disattivato"`).
- `DELETE /api/products/{id}`: Eliminazione fisica se richiesta.

### 4.4 Users (`/api/users`)
- `GET /api/users`: Supporta `_page`, `_limit` ed estrazione per credenziali: `?email={email}&password={password}` per autenticazione frontend immediata.
- `GET /api/users/{id}`: Dettaglio utente.
- `POST /api/users`: Registrazione nuovo utente.
- `PATCH /api/users/{id}`: Modifica dati/ruolo utente.
- `DELETE /api/users/{id}`: Eliminazione utente.

### 4.5 CustomerStats (`/api/customerStats`)
- `GET /api/customerStats`: Lista o filtro `?customerId={customerId}`.
- `GET /api/customerStats/{id}`: Dettaglio record statistico.
- `POST /api/customerStats`: Creazione record.
- `PATCH /api/customerStats/{id}`: Aggiornamento record.

---

## 5. Scheduler & Job Automatici

- **Classe**: `SubscriptionScheduler`
- **Configurazione**: `@EnableScheduling` in classe di configurazione principale.
- **Logica**:
  - Esecuzione all'avvio dell'applicazione (`@PostConstruct` o `CommandLineRunner`) e con cron giornaliero (`@Scheduled(cron = "0 0 1 * * ?")`).
  - Query: seleziona i clienti con `status = 'Attivo'` e `subscriptionEndDate < CURRENT_DATE`.
  - Azione: aggiorna lo stato a `'Scaduto'` e registra log di sistema.

---

## 6. Popolamento Dati Iniziali (Data Seeding)

- **Risorsa**: `src/main/resources/data/seed-db.json` (copia speculare di `dashboard-tesi/db.json`).
- **Comportamento di `DataInitializer`**:
  - All'avvio, verifica se `userRepository.count() == 0` o `customerRepository.count() == 0`.
  - Se vuoto, deserializza il file JSON tramite Jackson `ObjectMapper` e popola in sequenza:
    1. Utenti (`User`)
    2. Prodotti (`Product`)
    3. Clienti (`Customer`) con le relative date di abbonamento
    4. Ordini (`Order`) con i relativi `OrderItem`
    5. Statistiche clienti (`CustomerStats`)
  - Log di riepilogo con conteggi inseriti.

---

## 7. Strategia di Verifica e Test

1. **Test Unitari e di Integrazione**:
   - `QueryFilterParserTest`: test conversione filtri `_like`, `_page`, `_limit`, `_sort`.
   - `OrderServiceTest`: verifica atomicità creazione ordine + estensione abbonamento cliente + ricalcolo stats.
   - `StatsEndpointTest`: verifica calcolo revenue totale e mensile su dataset noto.
2. **Test End-to-End con Frontend**:
   - Avvio backend Spring Boot su `:8080`.
   - Avvio Angular con `npm run start:springboot` (puntamento a `http://localhost:8080/api`).
   - Verifica tabelle lazy-load PrimeNG: paginazione, ordinamento colonne, ricerca testuale.
   - Verifica creazione ordine, visualizzazione statistiche grafici dashboard e profilo utente.
