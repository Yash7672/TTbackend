# ---------------------------------------------------------------------------
# FIXORA backend image — multi-stage build
#   Stage 1: build the fat jar with Maven on JDK 17
#   Stage 2: run the COMPILED jar on a slim JRE (no source, no Maven)
# ---------------------------------------------------------------------------

# ------------------------------ build stage --------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the POM first so dependency downloads are cached between builds.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ------------------------------- run stage ----------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# curl is used by the Compose healthcheck on /api/health
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Run as a non-root user.
RUN groupadd --system fixora && useradd --system --gid fixora --create-home fixora

COPY --from=build /build/target/fixora-backend.jar /app/app.jar
RUN mkdir -p /app/uploads && chown -R fixora:fixora /app

USER fixora
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
