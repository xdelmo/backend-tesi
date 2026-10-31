# Specifiche Architetturali: Modelli Dati & Schemi JPA

Dettaglio tecnico delle entità JPA gestite in ApexFlow con Hibernate e PostgreSQL.

---

## 1. Entità `Customer` (`customers`)

Rappresenta un cliente aziendale abbonato ai servizi ApexFlow.

| Campo Java | Colonna DB | Tipo | Nullable | Descrizione / Vincoli |
|---|---|---|---|---|
| `id` | `id` | `VARCHAR(255)` | No | Chiave Primaria (16 caratteri generati o forniti) |
| `name` | `name` | `VARCHAR(255)` | No | Nome del cliente o referente |
| `email` | `email` | `VARCHAR(255)` | No | Indirizzo email |
| `company` | `company` | `VARCHAR(255)` | No | Ragione sociale |
| `status` | `status` | `VARCHAR(50)` | No | Stato cliente: `Attivo`, `In Attesa`, `Inattivo`, `Scaduto`, `Sospeso` |
| `industry` | `industry` | `VARCHAR(100)` | Sì | Settore (`Software`, `Creative`, `Logistics`, ecc.) |
| `plan` | `plan` | `VARCHAR(50)` | Sì | Piano tariffario (`Basic`, `Professional`, `Enterprise`) |
| `subscriptionEndDate` | `subscription_end_date` | `DATE` | Sì | Data di scadenza abbonamento (formato ISO `YYYY-MM-DD`) |

- **Proprietà Transitorie (`@Transient`)**:
  - `List<CustomerStats> customerStats`: caricato on-demand per serializzazione JSON embedded e calcolo dinamico dell'ordinamento per revenue.

---

## 2. Entità `Order` (`orders`) & `OrderItem` (`order_items`)

Rappresentano la transazione di acquisto e i singoli articoli inclusi.

### `Order` (`orders`)
| Campo Java | Colonna DB | Tipo | Nullable | Descrizione |
|---|---|---|---|---|
| `id` | `id` | `VARCHAR(255)` | No | PK identificativa (es. `ORD-719`) |
| `customerId` | `customer_id` | `VARCHAR(255)` | Sì | Riferimento logico all'ID del cliente |
| `date` | `date` | `TIMESTAMP` | No | Data e ora di creazione dell'ordine |
| `status` | `status` | `VARCHAR(50)` | No | Stato: `Pagato`, `In Attesa`, `Fallito` |
| `type` | `type` | `VARCHAR(50)` | No | Tipo: `Abbonamento`, `Una Tantum` |
| `subtotal` | `subtotal` | `DOUBLE PRECISION` | No | Imponibile |
| `tax` | `tax` | `DOUBLE PRECISION` | No | Imposte applicate |
| `discountAmount` | `discount_amount`| `DOUBLE PRECISION` | No | Sconto commerciale |
| `total` | `total` | `DOUBLE PRECISION` | No | Totale finale addebitato |

- **Proprietà Transitorie (`@Transient`)**:
  - `Customer customer`: popolato on-demand tramite query batch quando viene passato il parametro `_expand=customer` o durante la lettura per ID / creazione ordine. Evita query N+1 serializzando l'anagrafica cliente associata.

### `OrderItem` (`order_items`)
| Campo Java | Colonna DB | Tipo | Nullable | Descrizione |
|---|---|---|---|---|
| `id` | `id` | `BIGINT` | No | PK auto-incrementale (`IDENTITY`) |
| `productId` | `product_id` | `VARCHAR(255)` | No | ID prodotto (es. `PROD-CL-001`) |
| `name` | `name` | `VARCHAR(255)` | No | Nome del prodotto al momento dell'acquisto |
| `price` | `price` | `DOUBLE PRECISION` | No | Prezzo unitario applicato |
| `quantity` | `quantity` | `INTEGER` | No | Quantità acquistata |
| `category` | `category` | `VARCHAR(100)` | Sì | Categoria merceologica |
| `order` | `order_id` | `FK -> orders` | No | `@ManyToOne` relazione padre con `@JsonIgnore` |

---

## 3. Entità `Product` (`products`)

Catalogo dei prodotti e servizi offerti sulla piattaforma SaaS.

| Campo Java | Colonna DB | Tipo | Nullable | Descrizione |
|---|---|---|---|---|
| `id` | `id` | `VARCHAR(255)` | No | PK (es. `PROD-CL-001`) |
| `name` | `name` | `VARCHAR(255)` | No | Nome del prodotto |
| `description` | `description` | `VARCHAR(255)` | Sì | Descrizione tecnica |
| `category` | `category` | `VARCHAR(100)` | No | Categoria: `Cloud`, `Analytics`, `AI`, `Consulenza` |
| `price` | `price` | `DOUBLE PRECISION` | No | Prezzo base |
| `duration` | `duration` | `VARCHAR(50)` | No | Ricorrenza: `Mensile`, `Annuale`, `Una Tantum` |
| `status` | `status` | `VARCHAR(50)` | No | Stato: `Attivo`, `Bozza`, `Disattivato` |

---

## 4. Entità `User` (`users`)

Credenziali e profili di accesso amministrativo e operatore.

| Campo Java | Colonna DB | Tipo | Nullable | Descrizione |
|---|---|---|---|---|
| `id` | `id` | `BIGINT` | No | PK auto-incrementale |
| `email` | `email` | `VARCHAR(255)` | No | Indirizzo email (login) |
| `password` | `password` | `VARCHAR(255)` | No | Password utente |
| `name` | `name` | `VARCHAR(255)` | No | Nome visualizzato |
| `role` | `role` | `VARCHAR(50)` | No | Ruolo: `admin`, `user` |
| `avatar` | `avatar` | `VARCHAR(255)` | Sì | URL avatar PrimeNG |
| `createdAt` | `created_at` | `TIMESTAMP` | No | `@CreationTimestamp` Hibernate |
| `updatedAt` | `updated_at` | `TIMESTAMP` | No | `@UpdateTimestamp` Hibernate |

---

## 5. Entità `CustomerStats` (`customer_stats`)

Metriche aggregate calcolate per singolo cliente per alimentare la dashboard analitica.

| Campo Java | Colonna DB | Tipo | Nullable | Descrizione |
|---|---|---|---|---|
| `id` | `id` | `VARCHAR(255)` | No | PK (es. `cs-1767988302598`) |
| `customerId` | `customer_id` | `VARCHAR(255)` | No | ID cliente associato |
| `totalRevenue`| `total_revenue` | `DOUBLE PRECISION` | No | Totale spesa cumulativa |
| `totalOrders` | `total_orders` | `INTEGER` | No | Conteggio ordini effettuati |
| `lastOrderDate`| `last_order_date`| `TIMESTAMP` | Sì | Data e ora dell'ultimo ordine |
