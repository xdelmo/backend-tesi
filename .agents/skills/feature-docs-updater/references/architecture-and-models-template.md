# Architecture & Models Documentation Template

Use this standard when introducing or updating entities, database schemas, configurations, or architectural design decisions.

---

## 1. Data Model / Entity Documentation

Ogni nuova entità o modifica a tabelle/modelli esistenti deve includere:

### Modello: `NomeEntita` (`nome_tabella`)

**Descrizione**: Ruolo del modello nel dominio applicativo e ciclo di vita.

#### Struttura Campi & Attributi

| Campo Java | Colonna DB | Tipo | Nullable | Default | Descrizione / Vincoli |
|---|---|---|---|---|---|
| `id` | `id` | `String` / `Long` | No | - | Chiave Primaria (es. generata o prefissata `ORD-`) |
| `name` | `name` | `VARCHAR(150)` | No | - | Nome descrittivo |
| `totalAmount` | `total_amount` | `NUMERIC(12,2)` | No | `0.00` | Totale ordine calcolato in valuta standard |
| `status` | `status` | `VARCHAR(30)` | No | `'PENDING'` | Valori ammessi: `ATTIVO`, `IN ATTESA`, `SCADUTO` |
| `createdAt` | `created_at` | `TIMESTAMP` | No | `now()` | Timestamp di creazione gestito dal server |

#### Relazioni e Associazioni

- **Relazioni JPA esplicite**:
  - `Order 1 -> N OrderItem` (Cascade `ALL`, `orphanRemoval = true`, `mappedBy = "order"`).
  - Annotazioni di serializzazione: `@JsonIgnore` su `OrderItem.order` per prevenire ricorsione infinita bidirezionale.
- **Relazioni logiche per ID stringa**:
  - `Order.customerId` fa riferimento a `Customer.id` senza foreign key JPA gestita direttamente per mantenere il disaccoppiamento dei layer.

#### Regole di Business & Invarianti del Modello
- La transizione di stato da `ATTIVO` a `SCADUTO` può avvenire solo se `subscriptionEndDate < LocalDate.now()`.
- Il campo `totalAmount` deve coincidere con la somma dei singoli `OrderItem.price * quantity`.

---

## 2. Configuration & Environment Variables

Quando si aggiunge o modifica un parametro in `application.properties` o variabili d'ambiente:

| Proprietà Spring | Variabile d'Ambiente | Default | Obbligatoria | Scopo |
|---|---|---|---|---|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/apexflow_db` | No (in locale) | URL di connessione PostgreSQL |
| `server.port` | `PORT` | `8080` | No | Porta di ascolto del web server |
| `app.features.auto-expiry` | `AUTO_EXPIRY_ENABLED` | `true` | No | Abilita il cron job di scadenza automatica abbonamenti |

---

## 3. Modifiche ad Architettura e Linee Guida (CLAUDE.md)

Se la modifica introduce un nuovo pattern (es. aggiunta di DTO, transizione a `@Transactional(readOnly = true)`, filtri custom, o validatori):
1. **Aggiornare `CLAUDE.md`** nella sezione `## Architecture` o `## Conventions`.
2. Indicare esplicitamente cosa NON fare per prevenire regressioni da parte di altri agenti o sviluppatori.
3. Fornire un esempio di snippet del pattern approvato.
