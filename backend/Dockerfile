# Stage 1: build the Spring Boot jar (tests/coverage already run in CI)
FROM registry.access.redhat.com/ubi9/openjdk-17 AS build
WORKDIR /build
COPY --chown=185:0 pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY --chown=185:0 src ./src
RUN mvn --batch-mode --no-transfer-progress package -DskipTests

# Stage 2: slim runtime (non-root user 185, works with OpenShift random UIDs)
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime
COPY --from=build /build/target/notes-api-*.jar /deployments/app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
CMD ["java", "-jar", "/deployments/app.jar"]

