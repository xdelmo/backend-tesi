# 🔹 ApexFlow Backend - Spring Boot API

Backend RESTful API per l'applicazione **ApexFlow**, sviluppato con **Spring Boot 4** e **Java 21**. Fornisce servizi per la gestione di clienti, prodotti, ordini e statistiche, supportando l'architettura a microservizi e la scalabilità enterprise.

## 🚀 Funzionalità Principali

### 🔐 API & Sicurezza

- **RESTful Endpoints:** Architettura API standard per risorse `Customers`, `Products`, `Orders` e `Users`.
- **CORS Configuration:** Configurazione globale per consentire la comunicazione sicura con il frontend Angular.
- **Data Seeding:** Inizializzazione automatica del database con dati demo realistici (`DataInitializer`) per un setup immediato.

### 💼 Dominio di Business

- **Gestione Clienti:**
  - CRUD completo per le anagrafiche aziendali.
  - Tracking dello stato (Attivo, In Attesa, Inattivo) e del piano di sottoscrizione.
- **Catalogo Prodotti:**
  - Gestione servizi Cloud, Analytics e AI.
  - Attributi dinamici per pricing, durata e categoria.
- **Ordini & Transazioni:**
  - Modello relazionale complesso `Order` -> `OrderItem` (One-to-Many).
  - Gestione dettagliata importi (Subtotale, Tasse, Sconti).
- **Analytics Engine:**
  - Endpoint dedicato `/api/customerStats` per aggregazione dati dashboard.
  - Calcolo metriche in tempo reale.

### 🛠 Architettura Tecnica

- **Spring Boot 4:** Framework core per rapid application development.
- **Spring Data JPA:** Layer di persistenza astratto su **Hibernate**.
- **PostgreSQL Driver:** Configurazione pronta per database relazionali enterprise.
- **Lombok:** Riduzione boilerplate code per Entity e DTO.
- **Actuator:** Monitoraggio e metriche operative dell'applicazione.

---

## 🛠 Tech Stack

- **Linguaggio:** [Java 21](https://www.java.com/)
- **Framework:** [Spring Boot 4.0.1](https://spring.io/projects/spring-boot)
- **Database:** PostgreSQL (locale e cloud)
- **ORM:** Hibernate / Spring Data JPA
- **Build Tool:** Maven
- **Containerizzazione:** [Docker](https://www.docker.com/)

---

## 📂 Struttura del Progetto

```text
src/main/java/com/apexflow/
├── config/         # Configurazioni (CORS, Data Seeding)
├── controller/     # REST Controllers (API Layer)
├── model/          # JPA Entities (Domain Layer)
└── repository/     # Data Access Layer (Interfaces)
```

---

## ☁️ Cloud Deployment (Render.com)

L'applicazione è configurata per il deploy automatico su **Render** tramite il file `Dockerfile`.

- **Live API URL:** [https://backend-tesi.onrender.com/](https://backend-tesi.onrender.com/)
- **Database:** PostgreSQL gestito su Render.
- **CI/CD:** Ogni push sul branch `main` attiva automaticamente una nuova build e il deploy del container.

### Variabili d'Ambiente richieste

Per il corretto funzionamento in cloud, le seguenti variabili devono essere configurate nella dashboard di Render:

- `SPRING_DATASOURCE_URL`: URL JDBC del database PostgreSQL.
- `SPRING_DATASOURCE_USERNAME`: Username del database.
- `SPRING_DATASOURCE_PASSWORD`: Password del database.
- `PORT`: Porta interna dell'applicazione (default `8080`).

---

## ▶️ Setup & Installazione Locale

1.  **Clona il repository:**
    ```bash
    git clone https://github.com/xdelmo/backend-tesi.git
    ```
2.  **Compila il progetto:**
    Assicurati di avere JDK 21 installato e un PostgreSQL attivo su `localhost:5432` con database `apexflow_db` (credenziali di default `postgres` / `admin`, sovrascrivibili con le variabili `SPRING_DATASOURCE_*`).
    ```bash
    ./mvnw clean install
    ```
3.  **Avvia l'applicazione:**
    Il server si avvierà su `http://localhost:8080`
    ```bash
    ./mvnw spring-boot:run
    ```
4.  **Verifica Endpoints:**
    - Root (Welcome): `GET http://localhost:8080/`
    - Products: `GET http://localhost:8080/api/products`
    - Customers: `GET http://localhost:8080/api/customers`

---

**Autore:** Emanuele Del Monte
**Progetto:** Advanced Web Development / Tesi Backend API
