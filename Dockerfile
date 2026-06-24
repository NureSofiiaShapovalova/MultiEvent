FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace/app

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system eventflow \
    && useradd --system --gid eventflow --home-dir /app eventflow

COPY --from=build /workspace/app/target/*.jar /app/eventflow.jar

USER eventflow

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/eventflow.jar"]
