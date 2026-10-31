---
name: feature-docs-updater
description: Proattivamente attivata ad ogni inserimento, estensione o modifica di feature, endpoint REST, modello dati, logica di business o configurazione di progetto per mantenere la documentazione sempre allineata, esaustiva e ricca di dettagli tecnici. Utilizza OBBLIGATORIAMENTE questa skill ogni volta che viene implementata una nuova feature, aggiunto o modificato un endpoint HTTP, alterata un'entità di database, introdotto un parametro di configurazione, o prima di finalizzare un task, completare una PR o creare un commit. Si attiva su richieste come "aggiungi feature", "crea endpoint", "modifica modello", "aggiorna docs", "feature completata", "documenta", o dopo modifiche al codice applicativo.
---

# Feature Docs Updater

Questa skill garantisce che **ogni nuova funzionalità, endpoint o modifica al dominio applicativo sia immediatamente e dettagliatamente documentata**, eliminando il debito tecnico e la documentazione obsoleta fin dal primo istante.

---

## 🎯 Regola Fondamentale: "No Hand-Waving"

Non limitarti mai a descrizioni generiche o superficiali (es. *"Aggiunto endpoint per gli ordini"*). Ogni documentazione prodotta o aggiornata deve essere **tecnicamente precisa, autosufficiente e granulare**, includendo:
1. **Firma esatta**: Metodo HTTP, URI esatto, parametri di path e query con vincoli e valori di default.
2. **Payload e Schemi**: Esempi JSON realistici completi per richieste e risposte (inclusi codici HTTP 200, 201, 204, 400, 404, ecc.).
3. **Modello Dati**: Tipi colonna, nullable, vincoli relazionali, indici e convenzioni di serializzazione.
4. **Effetti Collaterali**: Transazioni, aggiornamenti a cascata, trigger logici o job schedulati.
5. **Configurazioni**: Nuove property, variabili d'ambiente, valori di default.

---

## 🔄 Flusso di Lavoro (Workflow Operativo)

Segui rigorosamente questo processo in 5 step ad ogni modifica di feature o codice:

### Step 1: Analisi del Diff e Identificazione delle Modifiche
Prima di scrivere o modificare la documentazione, ispeziona le modifiche al codice (tramite `git status`, `git diff` o l'elenco dei file toccati durante la sessione):
- **Controller / Router**: Quali endpoint sono stati aggiunti, rimossi o modificati? Quali annotazioni (`@GetMapping`, `@PostMapping`, `@PatchMapping`, `@RequestParam`, `@PathVariable`, `@Valid`) sono presenti?
- **Modelli / Entità / DTO**: Quali campi, tipi o associazioni sono cambiati? Ci sono annotazioni come `@JsonIgnore`, `@Column(nullable = false)` o vincoli di validazione?
- **Configurazioni**: Sono state modificate proprietà in `application.properties`, file YAML o variabili d'ambiente?
- **Logica di Business**: Sono state aggiunte annotazioni `@Transactional`, logiche di calcolo, filtri o task schedulati (`@Scheduled`)?

### Step 2: Matrice di Mappatura della Documentazione
Determina quali file devono essere aggiornati in base alla tipologia di modifica:

| Componente Modificato | File Primario da Aggiornare | File Secondario da Sincronizzare |
|---|---|---|
| **Nuovo Endpoint REST** | `README.md` (sezione API / Funzionalità) e `docs/` (specifica endpoint) | `CLAUDE.md` (se introduce convenzioni o nuovi path radice) |
| **Nuovo Modello / Modifica Schema** | `docs/` (sezione Data Model / Entità) | `CLAUDE.md` (convenzioni ID, associazioni JPA) e `README.md` |
| **Nuova Configurazione / Env Var** | `README.md` (sezione Setup / Configurazione) | `CLAUDE.md` (istruzioni di esecuzione per agenti) |
| **Flusso di Business o Job** | `README.md` (Funzionalità di Business) | `docs/` (design spec o architettura) |
| **Refactoring Architetturale** | `CLAUDE.md` (Linee guida architettura) | `README.md` (sezione Architettura Tecnica) |

### Step 3: Redazione Dettagliata della Documentazione
Applica gli standard definiti nei file di riferimento:
- Per gli endpoint REST, adotta la struttura definita in [references/api-documentation-template.md](references/api-documentation-template.md).
- Per modelli di dati, schemi DB e configurazioni, consulta [references/architecture-and-models-template.md](references/architecture-and-models-template.md).

Assicurati che:
- Tutti gli esempi JSON siano sintatticamente validi.
- Se il backend espone header speciali (es. `X-Total-Count` per paginazione) o gestisce errori con lo standard RFC 7807 (`Problem Details`), questi siano esplicitati con payload d'esempio.
- Siano riportati i comandi `curl` pronti per il collaudo locale.

### Step 4: Sincronizzazione di `README.md` e `CLAUDE.md`
- **In `README.md`**:
  - Aggiorna la sezione `## 🚀 Funzionalità Principali` inserendo la nuova capability sotto la corretta area tematica.
  - Se è stata aggiunta una rotta fondamentale o parametri di avvio, aggiorna gli snippet o la tabella delle variabili d'ambiente.
- **In `CLAUDE.md`**:
  - Se la nuova feature stabilisce una convenzione per i futuri agenti (es. formato risposte not-found, gestione transazioni, convenzione ID stringa vs Long), aggiorna la sezione `## Architecture` o `## Conventions`.

### Step 5: Verifica e Checklist di Chiusura
Esegui la checklist presente in [references/docs-sync-checklist.md](references/docs-sync-checklist.md):
- [ ] Endpoint e parametri combaciano al 100% con il codice.
- [ ] Payload JSON completi di tipi e vincoli.
- [ ] Esempi `curl` verificati e funzionanti.
- [ ] Link interni e formattazione tabelle Markdown perfetti.

---

## 📋 Esempi di Confronto (Anti-Pattern vs Best Practice)

### ❌ Documentazione Inadeguata (Scorretta)
```markdown
### Orders API
Abbiamo aggiunto l'endpoint per le statistiche degli ordini.
GET /api/orders/stats
```
*Problema:* Non specifica parametri, codici di risposta, forma del JSON restituito, filtri né esempi.

### ✅ Documentazione Dettagliata (Corretta)
```markdown
### `GET /api/orders/stats/monthly-revenue`

Restituisce l'aggregato mensile dei ricavi per un determinato anno solare.

#### Parametri di Query
| Parametro | Tipo | Obbligatorio | Default | Descrizione |
|---|---|---|---|---|
| `year` | `Integer` | No | Anno corrente | Anno solare su cui aggregare il fatturato |

#### Risposta 200 OK
```json
[
  { "month": "GEN", "revenue": 14250.00, "ordersCount": 38 },
  { "month": "FEB", "revenue": 18900.50, "ordersCount": 45 }
]
```

#### Esempio cURL
```bash
curl -s "http://localhost:8080/api/orders/stats/monthly-revenue?year=2026"
```
```

---

## 🔗 Risorse di Riferimento

- Specifiche API & Template REST: [references/api-documentation-template.md](references/api-documentation-template.md)
- Modelli Dati & Configurazioni: [references/architecture-and-models-template.md](references/architecture-and-models-template.md)
- Checklist di Validazione: [references/docs-sync-checklist.md](references/docs-sync-checklist.md)
