# Specifiche API: Clienti (`/api/customers`)

Modulo dedicato alla gestione anagrafica, contrattuale e degli abbonamenti dei clienti di ApexFlow.

---

## 1. `GET /api/customers`

Restituisce l'elenco paginato e filtrato dei clienti.

### Parametri di Query

| Parametro | In | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|---|
| `_page` | Query | `Integer` | No | `1` | Numero di pagina (1-indexed) |
| `_limit` | Query | `Integer` | No | `10` | Numero massimo di elementi per pagina |
| `_sort` | Query | `String` | No | - | Campo di ordinamento (es. `name`, `email`, `customerStats.totalRevenue`) |
| `_order` | Query | `String` | No | `asc` | Direzione di ordinamento (`asc` o `desc`) |
| `name_like` | Query | `String` | No | - | Filtro parziale case-insensitive sul nome |
| `email_like`| Query | `String` | No | - | Filtro parziale case-insensitive sull'email |
| `company_like`| Query | `String` | No | - | Filtro parziale case-insensitive sull'azienda |
| `status` | Query | `String` | No | - | Filtro esatto sullo stato (`Attivo`, `In Attesa`, `Inattivo`, `Scaduto`) |
| `industry` | Query | `String` | No | - | Settore merceologico (`Software`, `Creative`, ecc.) |
| `plan` | Query | `String` | No | - | Piano sottoscritto (`Basic`, `Professional`, `Enterprise`) |
| `_embed` | Query | `String` | No | - | Valore `customerStats`: include le statistiche associate nel JSON |

### Headers di Risposta
- `Content-Type: application/json`
- `X-Total-Count: <total_elements>`
- `Access-Control-Expose-Headers: X-Total-Count, Authorization`

### Risposta `200 OK`
```json
[
  {
    "id": "A1b2C3d4E5f6G7h8",
    "name": "Mario Rossi",
    "email": "mario@rossi.tech",
    "company": "Tech Spa",
    "status": "Attivo",
    "industry": "Software",
    "plan": "Enterprise",
    "subscriptionEndDate": "2027-01-21",
    "customerStats": [
      {
        "id": "cs-1767988302598",
        "customerId": "A1b2C3d4E5f6G7h8",
        "totalRevenue": 499.5,
        "totalOrders": 1,
        "lastOrderDate": "2026-01-09T19:51:42.598"
      }
    ]
  }
]
```

### Esempio cURL
```bash
curl -i "http://localhost:8080/api/customers?_page=1&_limit=10&status=Attivo&_sort=customerStats.totalRevenue&_order=desc"
```

---

## 2. `GET /api/customers/{id}`

Restituisce il dettaglio di un singolo cliente tramite ID con statistiche incorporate.

### Risposte HTTP
- `200 OK`: Restituisce l'oggetto cliente.
- `404 NOT FOUND`: Se il cliente specificato non esiste.

### Esempio cURL
```bash
curl -i "http://localhost:8080/api/customers/A1b2C3d4E5f6G7h8"
```

---

## 3. `POST /api/customers`

Crea un nuovo cliente nel sistema.

### Payload Richiesta
```json
{
  "name": "Acme Innovations Ltd",
  "email": "contact@acme.io",
  "company": "Acme Innovations",
  "status": "Attivo",
  "industry": "Software",
  "plan": "Professional",
  "subscriptionEndDate": "2027-06-30"
}
```

### Risposta `201 CREATED`
```json
{
  "id": "3f82a91b4c7d0e5f",
  "name": "Acme Innovations Ltd",
  "email": "contact@acme.io",
  "company": "Acme Innovations",
  "status": "Attivo",
  "industry": "Software",
  "plan": "Professional",
  "subscriptionEndDate": "2027-06-30",
  "customerStats": []
}
```

*Nota*: Se `id` non viene specificato nella richiesta, il backend genera automaticamente una stringa alfanumerica univoca di 16 caratteri.

### Esempio cURL
```bash
curl -X POST "http://localhost:8080/api/customers" \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme","email":"info@acme.io","status":"Attivo","company":"Acme Ltd"}'
```

---

## 4. `PATCH /api/customers/{id}`

Aggiornamento parziale delle proprietà del cliente o disattivazione (soft-delete).

### Payload Richiesta (Soft Delete)
```json
{
  "status": "Inattivo"
}
```

### Risposte HTTP
- `200 OK`: Restituisce l'entità cliente con i campi modificati.
- `404 NOT FOUND`: Se l'ID cliente non esiste.

### Esempio cURL
```bash
curl -X PATCH "http://localhost:8080/api/customers/A1b2C3d4E5f6G7h8" \
  -H "Content-Type: application/json" \
  -d '{"status":"Inattivo"}'
```

---

## 5. `DELETE /api/customers/{id}`

Eliminazione fisica del record cliente dal database.

### Risposte HTTP
- `204 NO CONTENT`: Eliminazione avvenuta con successo.
- `404 NOT FOUND`: Se il cliente non esiste.

### Esempio cURL
```bash
curl -X DELETE "http://localhost:8080/api/customers/A1b2C3d4E5f6G7h8"
```
