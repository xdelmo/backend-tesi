# Architettura del Seeding Dati (`DataInitializer`)

Documentazione del meccanismo di popolamento iniziale e ripristino del database per l'ambiente di sviluppo e produzione su Render.

---

## 1. Origine dei Dati e Sincronizzazione con il Frontend

Il dataset iniziale è archiviato in formato JSON all'interno delle risorse del backend:
- **Percorso file**: `src/main/resources/data/seed-db.json`
- **Sorgente**: Copia speculare e allineata al 100% con `dashboard-tesi/db.json`.

Contiene:
- **8 Clienti**: Compresi di settori industriali differenziati e date di scadenza abbonamento (`subscriptionEndDate`).
- **8 Prodotti**: Servizi Cloud, Analytics, AI e Consulenza con pricing e durate (Mensile, Annuale, Una Tantum).
- **7 Ordini**: Transazioni con array di `OrderItem` annidati (abbonamenti e ordini una tantum).
- **5 Statistiche Clienti**: Record aggregati di fatturato e conteggio ordini per cliente (`CustomerStats`).
- **2 Utenti**:
  - `admin@demo.com` / `password` (Ruolo `admin`)
  - `user@demo.com` / `password` (Ruolo `user`)

---

## 2. Meccanismo di Inizializzazione (`DataInitializer`)

- **Componente**: `com.apexflow.config.DataInitializer`
- **Trigger di esecuzione**: `CommandLineRunner` eseguito al completamento del bootstrap di Spring Boot.
- **Iniezione delle dipendenze**: Constructor injection rigorosa per tutti i 5 repository e l'istanza `ObjectMapper`.
- **Transazionalità**: Il metodo `public void seedDatabase()` è annotato con `@Transactional`, garantendo l'atomicità dell'intera sequenza di inserimento.
- **Logging**: SLF4J parametrizzato (`log.info("Seeding {}...", ...)`).

### Idempotenza e Controllo Esistenza
Per evitare duplicazioni o sovrascritture di dati già modificati dagli utenti, `DataInitializer` verifica preventivamente il conteggio di ciascuna tabella:
```java
if (customerRepository.count() == 0) {
    // Inserisce i clienti da seed-db.json
} else {
    log.info("Customers table is not empty ({} records), skipping.", count);
}
```

### Gestione Integrità Relazionale Ordini
Durante la deserializzazione dell'array di ordini, il codice itera sugli articoli associati impostando il back-reference `item.setOrder(order)` prima del salvataggio:
```java
for (Order order : seedData.getOrders()) {
    if (order.getItems() != null) {
        for (OrderItem item : order.getItems()) {
            item.setOrder(order);
        }
    }
    orderRepository.save(order);
}
```
Inoltre, per le entità con chiave numerica auto-incrementale (`User`), il campo `id` viene forzato a `null` (`user.setId(null)`) prima del `userRepository.save()` per delegare correttamente l'assegnazione alla sequenza identity di PostgreSQL.
