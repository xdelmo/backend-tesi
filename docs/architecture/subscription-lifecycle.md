# Architettura del Ciclo di Vita degli Abbonamenti

Documentazione del ciclo di vita degli abbonamenti ricorrenti (SaaS) in ApexFlow, dalla sottoscrizione tramite ordine alla scadenza automatica tramite scheduler.

---

## 1. Flusso del Ciclo di Vita

```
[ Creazione Ordine (POST /api/orders) ]
                  │
                  ▼
   [ Prodotto Mensile o Annuale? ]
         │                   │
      (Sì)                  (No)
         │                   │
         ▼                   ▼
[ Calcola subscriptionEndDate ]   [ Nessun impatto su abbonamento ]
[ Imposta Customer.status = 'Attivo' ]
                  │
                  ▼
       [ Cliente in Esercizio ]
                  │
                  ▼ (Trascorrere del tempo)
[ Cron Notturno (01:00 AM) / Startup ]
                  │
                  ▼
  [ subscriptionEndDate < Oggi? ]
         │                   │
      (Sì)                  (No)
         │                   │
         ▼                   ▼
[ Imposta Customer.status = 'Scaduto' ]  [ Resta 'Attivo' ]
```

---

## 2. Attivazione ed Estensione dell'Abbonamento (`OrderService`)

All'atto del salvataggio di un ordine (`createOrder`):
- Se l'ordine contiene uno o più articoli associati a prodotti con `duration = 'Mensile'` o `duration = 'Annuale'`:
  - Per ciascun articolo, calcola i mesi da sommare:
    - *Mensile*: `quantity * 1` mese.
    - *Annuale*: `quantity * 12` mesi.
  - La data di partenza del calcolo è:
    - La data odierna (`LocalDate.now()`), se il cliente non ha una scadenza attiva.
    - La data `subscriptionEndDate` preesistente, se quest'ultima è ancora futura (cumulabilità degli abbonamenti).
  - Viene forzato lo stato del cliente su `"Attivo"`.

---

## 3. Scheduler di Scadenza Automatica (`SubscriptionScheduler`)

- **Classe**: `com.apexflow.scheduler.SubscriptionScheduler`
- **Abilitazione**: `@EnableScheduling` in `ApexFlowApplication.java`.
- **Iniezione**: Constructor injection esclusiva con `private final CustomerRepository customerRepository`.
- **Logging**: SLF4J strutturato (`log.info("updating_expired_subscriptions count={}", ...)`).

### Frequenza di Esecuzione
1. **All'avvio dell'applicazione**: `@EventListener(ApplicationReadyEvent.class)` esegue un controllo immediato per sincronizzare lo stato dei clienti dopo riavvii o downtime.
2. **Cron Giornaliero**: `@Scheduled(cron = "0 0 1 * * ?")` esegue il job ogni notte alle ore 01:00.

### Condizione di Transizione
La query JPA Specification ricerca i record che soddisfano contemporaneamente:
```java
cb.and(
    cb.equal(root.get("status"), "Attivo"),
    cb.isNotNull(root.get("subscriptionEndDate")),
    cb.lessThan(root.get("subscriptionEndDate"), LocalDate.now())
)
```
Ciascun cliente identificato viene aggiornato a `status = "Scaduto"`.
L'operazione è **idempotente**: esecuzioni ripetute non modificano clienti già scaduti o con scadenze future.
