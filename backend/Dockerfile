# syntax=docker/dockerfile:1

# Build: compile and package the app module. Tests and the format check run in CI and
# with `./mvnw verify`, not in the image build.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY biwenger-client/pom.xml biwenger-client/
COPY app/pom.xml app/
COPY biwenger-client/src biwenger-client/src
COPY app/src app/src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl app -am package -DskipTests -Dspotless.check.skip=true

# Run: JRE only, as a non-root user.
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/app/target/app-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
