FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY pom.xml ./
COPY src/ src/
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

RUN groupadd --system bike && useradd --system --gid bike --home-dir /app bike \
    && mkdir -p /app/config /app/logs \
    && chown -R bike:bike /app

COPY --from=build --chown=bike:bike /workspace/target/demo-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --chown=bike:bike docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 0555 /app/docker-entrypoint.sh

USER bike
EXPOSE 8443 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
