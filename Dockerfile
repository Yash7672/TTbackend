
# ======================== BUILD STAGE ========================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ======================== RUNTIME STAGE ======================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user
RUN groupadd --system fixora \
    && useradd --system --gid fixora --create-home fixora

# Copy the compiled Spring Boot JAR
COPY --from=build /build/target/fixora-backend.jar /app/app.jar

# Entry point builds the Aiven truststore at startup, so no certificate or
# password is baked into the image.
COPY docker/entrypoint.sh /app/entrypoint.sh

# Writable certs/uploads dirs, then normalise CRLF and make the entrypoint runnable.
RUN mkdir -p /app/uploads /app/certs \
    && sed -i 's/\r$//' /app/entrypoint.sh \
    && chmod +x /app/entrypoint.sh \
    && chown -R fixora:fixora /app

# Linux truststore location used by the CA-verified JDBC connection. The
# password is a runtime secret and is deliberately NOT set here.
ENV SPRING_DATASOURCE_SSL_TRUSTSTORE_URL=file:/app/certs/aiven-truststore.p12 \
    SPRING_DATASOURCE_SSL_TRUSTSTORE_TYPE=PKCS12

# NOTE: the image deliberately does NOT set USER here. The container starts as
# root so docker/entrypoint.sh can read CA material that platforms mount
# root-only (Render Secret Files), build the truststore, and then drop
# privileges to the unprivileged `fixora` user before starting the JVM.

EXPOSE 8080

# PORT is assigned by Render and falls back to 8080 locally.
ENTRYPOINT ["/app/entrypoint.sh"]
