# Specifiche API: Catalogo Prodotti (`/api/products`)

Modulo per la gestione del catalogo prodotti e servizi offerti sulla piattaforma SaaS ApexFlow.

---

## 1. `GET /api/products`

Restituisce l'elenco paginato e filtrato dei prodotti.

### Parametri di Query

| Parametro | In | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|---|
| `_page` | Query | `Integer` | No | `1` | Numero di pagina (1-indexed) |
| `_limit` | Query | `Integer` | No | `10` | Numero massimo di elementi per pagina |
| `_sort` | Query | `String` | No | - | Campo di ordinamento (es. `name`, `price`, `category`) |
| `_order` | Query | `String` | No | `asc` | Direzione (`asc` o `desc`) |
| `name_like` | Query | `String` | No | - | Filtro parziale sul nome prodotto |
| `category` | Query | `String` | No | - | Categoria esatta (`Cloud`, `Analytics`, `AI`, `Consulenza`) |
| `status` | Query | `String` | No | - | Stato prodotto (`Attivo`, `Bozza`, `Disattivato`) |
| `duration` | Query | `String` | No | - | Ricorrenza (`Mensile`, `Annuale`, `Una Tantum`) |

### Headers di Risposta
- `Content-Type: application/json`
- `X-Total-Count: <total_elements>`
- `Access-Control-Expose-Headers: X-Total-Count, Authorization`

### Risposta `200 OK`
```json
[
  {
    "id": "PROD-CL-001",
    "name": "Archiviazione Enterprise",
    "description": "Storage cloud scalabile ad alta disponibilità",
    "category": "Cloud",
    "price": 500.0,
    "duration": "Mensile",
    "status": "Attivo"
  }
]
```

### Esempio cURL
```bash
curl -i "http://localhost:8080/api/products?_page=1&_limit=10&status=Attivo&_sort=price&_order=asc"
```

---

## 2. `GET /api/products/{id}`

Restituisce il dettaglio di un singolo prodotto.

### Risposte HTTP
- `200 OK`: Prodotto trovato.
- `404 NOT FOUND`: Se l'ID specificato non esiste.

### Esempio cURL
```bash
curl -i "http://localhost:8080/api/products/PROD-CL-001"
```

---

## 3. `POST /api/products`

Crea un nuovo prodotto nel catalogo. Se l'ID non viene fornito nel payload, il backend genera automaticamente un identificativo prefissato `PROD-XXXXXXXX`.

### Payload Richiesta
```json
{
  "name": "Cloud Security Suite",
  "description": "Protezione endpoint e firewall per ambienti cloud",
  "category": "Cloud",
  "price": 750.0,
  "duration": "Mensile",
  "status": "Attivo"
}
```

### Risposta `201 CREATED`
```json
{
  "id": "PROD-4E9A12B8",
  "name": "Cloud Security Suite",
  "description": "Protezione endpoint e firewall per ambienti cloud",
  "category": "Cloud",
  "price": 750.0,
  "duration": "Mensile",
  "status": "Attivo"
}
```

### Esempio cURL
```bash
curl -X POST "http://localhost:8080/api/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Cloud Security Suite",
    "category": "Cloud",
    "price": 750.0,
    "duration": "Mensile",
    "status": "Attivo"
  }'
```

---

## 4. `PATCH /api/products/{id}`

Aggiornamento parziale di uno o più campi del prodotto o disattivazione (soft-delete).

### Payload Richiesta (Soft Delete)
```json
{
  "status": "Disattivato"
}
```

### Risposte HTTP
- `200 OK`: Entità aggiornata.
- `404 NOT FOUND`: Se l'ID del prodotto non esiste.

### Esempio cURL
```bash
curl -X PATCH "http://localhost:8080/api/products/PROD-CL-001" \
  -H "Content-Type: application/json" \
  -d '{"status":"Disattivato"}'
```

---

## 5. `DELETE /api/products/{id}`

Eliminazione fisica del prodotto dal database.

### Risposte HTTP
- `204 NO CONTENT`: Eliminazione avvenuta con successo.
- `404 NOT FOUND`: Se il prodotto non esiste.

### Esempio cURL
```bash
curl -X DELETE "http://localhost:8080/api/products/PROD-CL-001"
```
