# 💻 Deploy & CI/CD

## Backend su Render

Render costruisce l'immagine dal `Dockerfile` a ogni push su `main` e la pubblica su [backend-tesi.onrender.com](https://backend-tesi.onrender.com/).

```dockerfile
# Dockerfile — build multi-stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Lo stage di build ha il JDK, quello finale solo il JRE: l'immagine pubblicata è più piccola. I test sono saltati (`-DskipTests`) perché nell'ambiente di build di Render non c'è PostgreSQL.

Il database e la porta arrivano dalle variabili `SPRING_DATASOURCE_*` e `PORT` configurate su Render.

!!! warning "Nessun gate di test"
    Il repository non ha una CI che lancia `./mvnw test`: i test vanno eseguiti in locale prima del push su `main`.

## Questo sito su GitHub Pages

`.github/workflows/docs.yml` parte a ogni push su `main` che tocca `docs/**`, `mkdocs.yml` o il workflow stesso: installa `mkdocs-material` ed esegue `mkdocs gh-deploy --force`, che pubblica sul branch `gh-pages`.
