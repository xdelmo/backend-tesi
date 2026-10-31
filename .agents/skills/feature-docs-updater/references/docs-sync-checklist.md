# Docs Sync Verification Checklist

Prima di considerare completata qualsiasi feature o modifica architetturale, verifica i seguenti punti:

### 1. Verifica Codice vs Documentazione
- [ ] Tutti i nuovi endpoint REST sono documentati con path esatto, metodo HTTP e query parameter ammessi.
- [ ] Il formato del payload di richiesta (JSON) corrisponde esattamente ai campi della classe/record DTO o Entity.
- [ ] Lo schema di risposta (codici HTTP 200, 201, 204, 400, 404, ecc.) e gli header esposti (es. `X-Total-Count`) sono riportati fedelmente.
- [ ] Se sono stati aggiunti nuovi campi a entità esistenti, sono stati documentati i tipi, vincoli di nullabilità e valori di default.
- [ ] Eventuali nuove proprietà in `application.properties` o variabili d'ambiente sono riflesse nelle istruzioni di configurazione.

### 2. File da Aggiornare
- [ ] **README.md**:
  - Aggiunta la nuova feature nell'elenco delle funzionalità (`## Funzionalità Principali`).
  - Se è un endpoint primario o cambia l'esecuzione locale, aggiornati gli snippet o le variabili d'ambiente.
- [ ] **CLAUDE.md**:
  - Aggiornate le convenzioni architetturali se sono cambiati pattern, annotazioni richieste o vincoli su controller/repository/service.
  - Aggiornati comandi di test/build se sono stati introdotti nuovi profili o flag.
- [ ] **File dedicati in `docs/`**:
  - Aggiornato o creato il documento di specifica API o design architetturale nella cartella `docs/`.
  - Se presente un changelog o release log, aggiunta la voce corrispondente.

### 3. Integrità e Formattazione
- [ ] I blocchi di codice specificano il linguaggio corretto per la sintassi (`bash`, `json`, `java`, `properties`).
- [ ] Le tabelle Markdown sono formattate correttamente e leggibili.
- [ ] I link relativi a file o risorse esistenti sono validi.
- [ ] Non sono presenti placeholder non risolti (es. `<TODO>`, `<DA_DEFINIRE>`).
