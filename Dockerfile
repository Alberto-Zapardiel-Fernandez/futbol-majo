# ── FASE 1: CONSTRUCCIÓN ─────────────────────────────────────────────────────
# Descargamos una imagen de Linux con Java 21 y Maven ya instalados.
# "AS build" le da un nombre a esta fase para usarla después.
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiamos primero solo el pom.xml para aprovechar la caché de Docker:
# si el pom.xml no cambia entre deploys, no re-descarga dependencias.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Ahora sí copiamos el código y compilamos
COPY src src
RUN ./mvnw clean package -DskipTests -q

# ── FASE 2: EJECUCIÓN ────────────────────────────────────────────────────────
# Imagen más ligera — solo el runtime de Java, sin Maven ni JDK completo.
# El contenedor final ocupa ~200MB en vez de ~700MB.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiamos solo el JAR compilado de la fase anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Arrancamos Spring Boot con límite de memoria para el free tier de Render
# -Xmx400m: máximo 400MB de heap (el free tier tiene 512MB en total)
ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar"]