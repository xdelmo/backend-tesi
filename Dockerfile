# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
# Prova a compilare il progetto usando il wrapper maven incluso
RUN ./mvnw clean package -DskipTests

# Stage 2: Execution
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copia il file JAR generato dallo stage precedente
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Avvia l'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]
