# Specifiche API: Statistiche Clienti (`/api/customerStats`)

Modulo dedicato alla gestione e consultazione delle metriche aggregate per singolo cliente, a supporto delle analisi di spesa e fidelizzazione nella dashboard ApexFlow.

---

## 1. `GET /api/customerStats`

Restituisce l'elenco paginato e filtrabile delle statistiche cliente.

### Parametri di Query

| Parametro | In | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|---|
| `_page` | Query | `Integer` | No | `1` | Numero di pagina (1-indexed) |
| `_limit` | Query | `Integer` | No | `10` | Numero massimo di elementi per pagina |
| `customerId` | Query | `String` | No | - | Filtra le statistiche per ID cliente specifico (usato da `CustomerStatsService.ts`) |
| `_sort` | Query | `String` | No | - | Campo di ordinamento (es. `totalRevenue`, `totalOrders`, `lastOrderDate`) |
| `_order` | Query | `String` | No | `asc` | Direzione (`asc` o `desc`) |

### Headers di Risposta
- `Content-Type: application/json`
- `X-Total-Count: <total_elements>`
- `Access-Control-Expose-Headers: X-Total-Count, Authorization`

### Esempio cURL Filtro per Cliente
```bash
curl -i "http://localhost:8080/api/customerStats?customerId=I9j0K1l2M3n4O5p6"
```

### Risposta `200 OK`
```json
[
  {
    "id": "cs-1767988302598",
    "customerId": "I9j0K1l2M3n4O5p6",
    "totalRevenue": 499.5,
    "totalOrders": 1,
    "lastOrderDate": "2026-01-09T19:51:42.598"
  }
]
```

---

## 2. `GET /api/customerStats/{id}`

Restituisce il dettaglio di una statistica cliente dato il suo ID primario.

### Risposte HTTP
- `200 OK`: Record statistico trovato.
- `404 NOT FOUND`: Se il record non esiste.

### Esempio cURL
```bash
curl -i "http://localhost:8080/api/customerStats/cs-1767988302598"
```

---

## 3. `POST /api/customerStats`

Crea o registra manualmente un record statistico per un cliente (solitamente generato in automatico durante la creazione dell'ordine).
Se il campo `id` è nullo o vuoto, il backend genera automaticamente un ID univoco prefissato `cs-XXXXXXXX`.

### Payload Richiesta
```json
{
  "customerId": "A1b2C3d4E5f6G7h8",
  "totalRevenue": 1250.0,
  "totalOrders": 3,
  "lastOrderDate": "2026-03-21T14:30:00"
}
```

### Risposta `201 CREATED`
```json
{
  "id": "cs-49f8a12e",
  "customerId": "A1b2C3d4E5f6G7h8",
  "totalRevenue": 1250.0,
  "totalOrders": 3,
  "lastOrderDate": "2026-03-21T14:30:00"
}
```

---

## 4. `PATCH /api/customerStats/{id}`

Aggiornamento parziale di uno o più valori statistici.

### Payload Richiesta
```json
{
  "totalRevenue": 1500.0,
  "totalOrders": 4
}
```

### Risposte HTTP
- `200 OK`: Record aggiornato.
- `404 NOT FOUND`: Se l'ID specificato non esiste.

### Esempio cURL
```bash
curl -X PATCH "http://localhost:8080/api/customerStats/cs-1767988302598" \
  -H "Content-Type: application/json" \
  -d '{"totalRevenue": 750.0}'
```
