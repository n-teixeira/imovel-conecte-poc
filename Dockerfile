# ImovelConecte - Dockerfile multi-stage

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copia pom.xml e baixa dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia código e compila
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuário não-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copia o JAR da etapa de build
COPY --from=builder /app/target/*.jar app.jar

# Variáveis de ambiente padrão
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
