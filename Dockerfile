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
    && groupadd --system multievent \
    && useradd --system --gid multievent --home-dir /app multievent

COPY --from=build /workspace/app/target/*.jar /app/multievent.jar

USER multievent

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/multievent.jar"]
