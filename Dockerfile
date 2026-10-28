# --- Etap 1: Budowanie aplikacji ---
FROM maven:3.10.0-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Kopiowanie zależności (optymalizacja cache Dockera)
COPY pom.xml .
RUN mvn dependency:go-offline

# Kopiowanie kodu źródłowego i budowanie pliku JAR
COPY src ./src
COPY api ./api
RUN mvn package -DskipTests

# --- Etap 2: Obraz uruchomieniowy ---
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Kopiowanie wygenerowanego pliku JAR z pierwszego etapu
COPY --from=build /app/target/*.jar app.jar

# Tworzenie użytkownika bez uprawnień roota (Bezpieczeństwo)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]