FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean verify

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system lms && useradd --system --gid lms lms
COPY --from=build --chown=lms:lms /app/target/lms-lite-0.0.1-SNAPSHOT.jar app.jar
USER lms
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
