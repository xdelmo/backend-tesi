# Specifiche API: Ordini & Statistiche (`/api/orders`)

Modulo dedicato alla registrazione degli ordini di acquisto, alla gestione automatica degli abbonamenti e al calcolo delle statistiche di fatturato.

---

## 1. `GET /api/orders`

Restituisce l'elenco paginato e filtrato degli ordini.

### Parametri di Query

| Parametro | In | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|---|
| `_page` | Query | `Integer` | No | `1` | Numero di pagina (1-indexed) |
| `_limit` | Query | `Integer` | No | `10` | Numero massimo di record per pagina |
| `_sort` | Query | `String` | No | - | Campo di ordinamento (es. `date`, `total`) |
| `_order` | Query | `String` | No | `asc` | Direzione di ordinamento (`asc` o `desc`) |
| `_expand` | Query | `String` | No | - | Espande la risorsa genitore associata (es. `customer` popola l'oggetto completo del cliente) |
| `customer.name_like` | Query | `String` | No | - | Filtro parziale sul nome del cliente collegato all'ordine |
| `status` | Query | `String` | No | - | Stato ordine (`Pagato`, `In Attesa`, `Fallito`) |
| `type` | Query | `String` | No | - | Tipologia (`Abbonamento`, `Una Tantum`) |

### Headers di Risposta
- `Content-Type: application/json`
- `X-Total-Count: <total_elements>`
- `Access-Control-Expose-Headers: X-Total-Count, Authorization`

### Risposta `200 OK` (con `_expand=customer`)
```json
[
  {
    "id": "ORD-719",
    "customerId": "I9j0K1l2M3n4O5p6",
    "date": "2026-01-09T19:51:34.467",
    "status": "Pagato",
    "type": "Abbonamento",
    "subtotal": 500.0,
    "tax": 49.5,
    "discountAmount": 50.0,
    "total": 499.5,
    "customer": {
      "id": "I9j0K1l2M3n4O5p6",
      "name": "Luca Bianchi",
      "email": "l.bianchi@consulting.it",
      "company": "Bianchi Consulting",
      "status": "Attivo",
      "industry": "Professional Services",
      "plan": "Enterprise"
    },
    "items": [
      {
        "id": 1,
        "productId": "PROD-CL-001",
        "name": "Archiviazione Enterprise",
        "price": 500.0,
        "quantity": 1,
        "category": "Cloud"
      }
    ]
  }
]
```

### Esempio cURL
```bash
curl -i "http://localhost:8080/api/orders?_page=1&_limit=10&status=Pagato&_sort=date&_order=desc"
```

---

## 2. `GET /api/orders/{id}`

Restituisce un singolo ordine con la lista dettagliata dei suoi articoli.

### Risposte HTTP
- `200 OK`: Ordine trovato.
- `404 NOT FOUND`: Se l'ordine non esiste.

### Esempio cURL
```bash
curl -i "http://localhost:8080/api/orders/ORD-719"
```

---

## 3. `POST /api/orders`

Crea un nuovo ordine eseguendo una transazione atomica `@Transactional` che include:
1. Salvataggio dell'ordine e associazione bidirezionale dei relativi `OrderItem`.
2. Verifica della presenza di prodotti in abbonamento (`Mensile` o `Annuale`):
   - Se presenti, aggiorna `Customer.subscriptionEndDate` estendendo la data di `quantity * 1 mese` o `quantity * 12 mesi`.
   - Se il cliente ha già una scadenza futura, la nuova data viene calcolata sommando i mesi a partire da quella data.
   - Lo stato del cliente viene forzato ad `"Attivo"`.
3. Creazione o incremento delle metriche in `CustomerStats` (`totalOrders += 1`, `totalRevenue += total`, `lastOrderDate = date`).

### Payload Richiesta
```json
{
  "customerId": "I9j0K1l2M3n4O5p6",
  "date": "2026-03-20T11:00:00",
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
      "quantity": 2,
      "category": "Cloud"
    }
  ]
}
```

### Risposta `201 CREATED`
```json
{
  "id": "ORD-4821",
  "customerId": "I9j0K1l2M3n4O5p6",
  "date": "2026-03-20T11:00:00",
  "status": "Pagato",
  "type": "Abbonamento",
  "subtotal": 500.0,
  "tax": 49.5,
  "discountAmount": 50.0,
  "total": 499.5,
  "items": [
    {
      "id": 15,
      "productId": "PROD-CL-001",
      "name": "Archiviazione Enterprise",
      "price": 500.0,
      "quantity": 2,
      "category": "Cloud"
    }
  ]
}
```

### Esempio cURL
```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "I9j0K1l2M3n4O5p6",
    "status": "Pagato",
    "type": "Abbonamento",
    "total": 499.5,
    "items": [{"productId":"PROD-CL-001","quantity":1,"price":500.0,"category":"Cloud","name":"Storage"}]
  }'
```

---

## 4. `DELETE /api/orders/{id}`

Elimina un ordine e tutti gli `OrderItem` associati a cascata.

### Risposte HTTP
- `204 NO CONTENT`: Eliminazione riuscita.
- `404 NOT FOUND`: Ordine non trovato.

---

## 5. `GET /api/orders/stats/revenue`

Restituisce la somma complessiva del fatturato degli ordini con stato `"Pagato"`.

### Risposta `200 OK`
```json
{
  "totalRevenue": 1698.3
}
```

### Esempio cURL
```bash
curl -s "http://localhost:8080/api/orders/stats/revenue"
```

---

## 6. `GET /api/orders/stats/monthly-revenue`

Restituisce un array con il fatturato mensile (12 posizioni da 0 a 11, per i mesi da Gennaio a Dicembre) per un determinato anno solare.

### Parametri di Query

| Parametro | In | Tipo | Obbligatorio | Descrizione |
|---|---|---|---|---|
| `year` | Query | `Integer` | **Sì** | Anno solare su cui aggregare il fatturato (es. `2026`) |

### Risposte HTTP
- `200 OK`: Array di 12 numeri floating-point.
- `400 BAD REQUEST`: Se il parametro `year` è omesso (`{"error": "Il parametro 'year' è obbligatorio"}`).

### Risposta `200 OK`
```json
[0.0, 499.5, 1198.8, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
```

### Esempio cURL
```bash
curl -s "http://localhost:8080/api/orders/stats/monthly-revenue?year=2026"
```
