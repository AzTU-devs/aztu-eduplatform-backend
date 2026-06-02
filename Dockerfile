# ---------- build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Cache dependencies first
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Build
COPY src/ src/
RUN ./mvnw -q -B -DskipTests package \
    && mkdir -p target/extracted \
    && java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-jammy

# Run as a non-root user
RUN groupadd --system app && useradd --system --gid app --create-home --home /home/app app
USER app
WORKDIR /app

# Copy Spring Boot layers (deps change least → app code most)
COPY --from=build --chown=app:app /workspace/target/extracted/dependencies/         ./
COPY --from=build --chown=app:app /workspace/target/extracted/spring-boot-loader/   ./
COPY --from=build --chown=app:app /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/application/          ./

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
