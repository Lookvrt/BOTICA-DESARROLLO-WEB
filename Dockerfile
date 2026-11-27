# Etapa 1: Compilación
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copiar archivos de Maven
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw

# Descargar dependencias
RUN ./mvnw dependency:go-offline -B || true

# Copiar código fuente y empaquetar JAR
COPY src src
RUN ./mvnw clean package -DskipTests

# Etapa 2: Imagen de ejecución ligera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios: si la app se compromete, no tiene permisos de root en el contenedor
RUN addgroup -S app && adduser -S -G app app

COPY --from=builder --chown=app:app /app/target/*.jar app.jar

ENV PORT=8080
ENV TZ=America/Lima
EXPOSE 8080
USER app

ENTRYPOINT ["sh", "-c", "java -Duser.timezone=America/Lima -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar"]