# Specifiche API: Gestione Utenti & Autenticazione (`/api/users`)

Modulo per la gestione degli account operatore/amministratore e per la compatibilità con il meccanismo di login del frontend.

---

## 1. `GET /api/users`

Restituisce l'elenco paginato degli utenti oppure esegue la ricerca per credenziali per il login frontend.

### Parametri di Query

| Parametro | In | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|---|
| `_page` | Query | `Integer` | No | `1` | Numero di pagina (1-indexed) |
| `_limit` | Query | `Integer` | No | `10` | Numero massimo di elementi per pagina |
| `email` | Query | `String` | No | - | Ricerca per email (usata da `AuthService.ts`) |
| `password` | Query | `String` | No | - | Ricerca per password (usata da `AuthService.ts`) |
| `role` | Query | `String` | No | - | Filtro per ruolo (`admin`, `user`) |
| `name_like` | Query | `String` | No | - | Filtro parziale sul nome dell'utente |

### Headers di Risposta
- `Content-Type: application/json`
- `X-Total-Count: <total_elements>`
- `Access-Control-Expose-Headers: X-Total-Count, Authorization`

### Esempio Chiamata Login Frontend (`AuthService.login`)
```bash
curl -i "http://localhost:8080/api/users?email=admin@demo.com&password=password"
```

### Risposta `200 OK`
```json
[
  {
    "id": 1,
    "email": "admin@demo.com",
    "password": "password",
    "name": "Utente Admin",
    "role": "admin",
    "avatar": "https://primefaces.org/cdn/primeng/images/demo/avatar/amyelsner.png",
    "createdAt": "2026-02-26T10:19:35.273",
    "updatedAt": "2026-02-26T10:19:49.684"
  }
]
```

---

## 2. `GET /api/users/{id}`

Restituisce il profilo di un utente dato il suo ID numerico.

### Risposte HTTP
- `200 OK`: Profilo utente trovato.
- `404 NOT FOUND`: Se l'ID specificato non esiste.

### Esempio cURL
```bash
curl -i "http://localhost:8080/api/users/1"
```

---

## 3. `POST /api/users`

Registra un nuovo utente nel sistema.

### Payload Richiesta
```json
{
  "email": "mario.rossi@apexflow.it",
  "password": "secretPassword123",
  "name": "Mario Rossi",
  "role": "user",
  "avatar": "https://primefaces.org/cdn/primeng/images/demo/avatar/onyamalimba.png"
}
```

### Risposta `201 CREATED`
```json
{
  "id": 2,
  "email": "mario.rossi@apexflow.it",
  "password": "secretPassword123",
  "name": "Mario Rossi",
  "role": "user",
  "avatar": "https://primefaces.org/cdn/primeng/images/demo/avatar/onyamalimba.png",
  "createdAt": "2026-10-31T12:50:00.123",
  "updatedAt": "2026-10-31T12:50:00.123"
}
```

### Esempio cURL
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{"email":"operatore@demo.com","password":"pwd","name":"Operatore","role":"user"}'
```

---

## 4. `PATCH /api/users/{id}`

Aggiornamento parziale delle informazioni dell'utente (nome, email, avatar, password, ruolo).
Se il campo `password` nella richiesta è vuoto o composto da soli spazi, viene ignorato per evitare cancellazioni involontarie della password.

### Payload Richiesta
```json
{
  "name": "Mario Rossi (Senior Admin)",
  "role": "admin"
}
```

### Risposte HTTP
- `200 OK`: Profilo aggiornato.
- `404 NOT FOUND`: Se l'utente con l'ID specificato non esiste.

### Esempio cURL
```bash
curl -X PATCH "http://localhost:8080/api/users/2" \
  -H "Content-Type: application/json" \
  -d '{"name":"Mario Rossi Senior"}'
```

---

## 5. `DELETE /api/users/{id}`

Elimina un account utente.

### Risposte HTTP
- `204 NO CONTENT`: Account eliminato.
- `404 NOT FOUND`: Utente non trovato.

### Esempio cURL
```bash
curl -X DELETE "http://localhost:8080/api/users/2"
```
