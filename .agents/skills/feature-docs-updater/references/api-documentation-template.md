# API Documentation Template & Standards

Use this standard whenever documenting new or updated REST endpoints in `README.md`, `CLAUDE.md`, or files in `docs/`.

---

## 1. REST Endpoint Specification Format

For every endpoint, provide all of the following sections without skipping details:

### `[HTTP_METHOD] /api/v1/resource-path`

**Descrizione**: Spiegazione chiara dello scopo dell'endpoint e del contesto di business.

#### Parametri di Query / Path

| Parametro | In | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|---|
| `id` | Path | `String` | Sì | - | Identificativo univoco della risorsa (es. `PROD-CL-001`) |
| `_page` | Query | `Integer` | No | `1` | Numero di pagina (1-indexed) |
| `_limit` | Query | `Integer` | No | `10` | Numero massimo di elementi per pagina |
| `_sort` | Query | `String` | No | `id` | Campo di ordinamento |
| `_order` | Query | `String` | No | `asc` | Direzione di ordinamento (`asc` o `desc`) |
| `q` | Query | `String` | No | - | Query di ricerca full-text multi-campo |

#### Headers di Richiesta / Risposta

- **Request**:
  - `Content-Type: application/json`
  - *(Se applicabile)* `Authorization: Bearer <token>`
- **Response**:
  - `Content-Type: application/json`
  - *(Se paginato)* `X-Total-Count: <total_records>`
  - *(Se paginato)* `Access-Control-Expose-Headers: X-Total-Count`

#### Request Body (se POST / PUT / PATCH)

```json
{
  "campoObbligatorio": "valore stringa",
  "importo": 149.99,
  "attivo": true,
  "dettagli": {
    "chiave": "valore"
  }
}
```

*Note su vincoli di validazione:*
- `campoObbligatorio`: non nullo, lunghezza max 100 char.
- `importo`: decimale positivo (`BigDecimal`), minimo `0.00`.

#### Risposte HTTP

##### 200 OK / 201 CREATED (Successo)
```json
{
  "id": "ORD-2026-001",
  "clienteId": "CUST-042",
  "totale": 149.99,
  "stato": "CONFERMATO",
  "dataCreazione": "2026-10-31T12:00:00Z"
}
```

##### 204 NO CONTENT (Per cancellazioni o azioni senza payload)
Nessun corpo di risposta restituito.

##### 400 BAD REQUEST (Validazione / Input non valido - RFC 7807)
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Il parametro 'totale' non può essere negativo",
  "instance": "/api/orders"
}
```

##### 404 NOT FOUND (Risorsa non trovata)
*(A seconda della convenzione di progetto: empty body con 200 oppure RFC 7807 404 - specificare esplicitamente).*

#### Esempio di Chiamata cURL

```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "total": 299.90,
    "status": "Attivo"
  }'
```

#### Effetti Collaterali & Logica Transazionale
- Aggiorna automaticamente il record `CustomerStats` corrispondente per ricalcolare `totalSpent` e `ordersCount`.
- Se il prodotto acquistato è ricorrente, estende `subscriptionEndDate` del cliente.
