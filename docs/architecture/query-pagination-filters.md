# Architettura di Querying, Paginazione e Filtri

Approfondimento tecnico sul funzionamento di `QueryFilterParser` e l'integrazione tra le convenzioni frontend e Spring Data JPA.

---

## 1. Obiettivo & Flusso di Elaborazione

Il frontend Angular invia parametri query compatibili con le convenzioni di `json-server`. La classe `QueryFilterParser` intercetta questi parametri e li mappa senza perdita di semantica su:
1. `org.springframework.data.domain.Pageable` per delegare paginazione e ordinamento a PostgreSQL (`LIMIT`, `OFFSET`, `ORDER BY`).
2. `org.springframework.data.jpa.domain.Specification<T>` per generare dinamicamente le clausole `WHERE` tramite JPA `CriteriaBuilder`.

```
[ Parametri HTTP: _page, _limit, _sort, name_like, status ]
                          │
                          ▼
             [ QueryFilterParser ]
             ├──► getPageable(params)    ==> PageRequest(page-1, limit, sort)
             └──► parse(params)          ==> Specification<T> (Criteria API)
                          │
                          ▼
[ Repository.findAll(Specification, Pageable) ]
                          │
                          ▼
            [ SQL eseguito su PostgreSQL ]
SELECT * FROM table WHERE lower(name) LIKE '%tech%' AND status = 'Attivo' LIMIT 10 OFFSET 0;
```

---

## 2. Regole di Mappatura dei Parametri

### 2.1 Paginazione
- `_page`: il frontend invia la numerazione 1-based (es. pagina `1`). Spring Data adotta la numerazione 0-based. La conversione applica `Math.max(0, page - 1)`.
- `_limit`: specifica la dimensione pagina (default `10`).

### 2.2 Ordinamento
- `_sort`: nome dell'attributo dell'entità (es. `date`, `name`, `price`). Se contiene caratteri `.` (es. ordinamenti cross-table complessi), viene delegato alla logica specializzata del Service (es. `customerStats.totalRevenue`).
- `_order`: direzione ordinamento (`asc` o `desc`).

### 2.3 Filtri Parziali (`*_like`)
- Qualsiasi parametro con suffisso `_like` (es. `company_like=Tech`) viene convertito in un predicato case-insensitive:
  ```java
  cb.like(cb.lower(root.get("company")), "%" + value.toLowerCase() + "%")
  ```

### 2.4 Filtri Esatti
- Parametri corrispondenti ad attributi validi dell'entità privi di suffisso speciale generano un predicato di uguaglianza:
  ```java
  cb.equal(root.get(attributeName), value)
  ```

### 2.5 Chiavi Riservate
I parametri di controllo (`_page`, `_limit`, `_sort`, `_order`, `_embed`, `_expand`, `_refresh`, `q`) sono esclusi automaticamente dalla generazione dei predicati standard per evitare conflitti o errori di parsing degli attributi.

---

## 3. Header `X-Total-Count` e CORS

Tutti i controller REST che restituiscono collezioni paginate calcolano il totale degli elementi disponibili dal `Page<T>` e lo iniettano nell'header HTTP:
```java
HttpHeaders headers = new HttpHeaders();
headers.set("X-Total-Count", String.valueOf(page.getTotalElements()));
return ResponseEntity.ok().headers(headers).body(page.getContent());
```

In `SecurityConfig`, il bean `CorsConfiguration` espone esplicitamente tale header:
```java
config.setExposedHeaders(List.of("X-Total-Count", "Authorization"));
```
Questo consente a `CustomerService` e `OrderService` in Angular di leggere `response.headers.get('X-Total-Count')` per impostare correttamente la paginazione di PrimeNG `p-table`.

---

## 4. Embedding ed Espansione Risorse Collegate (`_embed` e `_expand`)

ApexFlow supporta le due convenzioni REST essenziali per il client SPA senza introdurre N+1 query:

### 4.1 Embedding Figli (`_embed=customerStats`)
- **Contesto**: `GET /api/customers?_embed=customerStats`
- **Comportamento**: Popola per ciascun cliente paginato la proprietà transitoria `Customer.customerStats` caricando le metriche aggregate associate, consentendo a `customer-list.component.html` di mostrare il fatturato generato da ciascun cliente.

### 4.2 Espansione Genitore (`_expand=customer`)
- **Contesto**: `GET /api/orders?_expand=customer`
- **Comportamento**: Risolve per ogni ordine la relazione col genitore associato `Order.customerId`. Per garantire alte prestazioni e scongiurare N+1 query:
  1. Estrae l'insieme di tutti i `customerId` unici dalla pagina di ordini.
  2. Esegue una singola query batch `customerRepository.findAllById(customerIds)`.
  3. Mappa in memoria ogni `Customer` sull'attributo `@Transient Order.customer`.
- Consente al frontend Angular (`order-list.component.html`) di accedere direttamente a `{{ order.customer.name }}` senza dover effettuare ulteriori chiamate HTTP asincrone.
