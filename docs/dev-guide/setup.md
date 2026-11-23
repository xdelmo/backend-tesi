# 💻 Setup & Test

## Requisiti

- Java 21 (il Maven Wrapper scarica Maven da solo)
- PostgreSQL raggiungibile, in locale o remoto

## Configurazione

`src/main/resources/application.properties` legge tutto da variabili d'ambiente, con default per lo sviluppo locale:

| Variabile | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/apexflow_db` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `admin` |
| `PORT` | `8080` |

Lo schema lo crea Hibernate (`spring.jpa.hibernate.ddl-auto=update`); al primo avvio `DataInitializer` carica i dati demo da `src/main/resources/data/seed-db.json` solo nelle tabelle vuote.

## Comandi

```bash
./mvnw spring-boot:run                     # avvia su http://localhost:8080
./mvnw test                                # tutti i test
./mvnw test -Dtest=CustomerControllerTest  # una sola classe
./mvnw clean install                       # build + test
```

!!! note "I test usano il database"
    Le classi di test sono `@SpringBootTest` e non c'è un profilo di test separato: anche `./mvnw test` ha bisogno di PostgreSQL raggiungibile con la configurazione sopra.

## Collegare la dashboard

Dalla cartella del frontend `dashboard-tesi`:

```bash
npm run start:springboot
```

La dashboard gira su `http://localhost:4200` e chiama questo backend su `8080`.

## Docs

Questo sito è MkDocs Material. Per vederlo in locale:

```bash
pip install mkdocs-material
mkdocs serve
```

Una nuova feature o un cambio di comportamento aggiorna il `README.md` e le pagine di `docs/`; le pagine nuove vanno registrate nella `nav` di `mkdocs.yml`.
