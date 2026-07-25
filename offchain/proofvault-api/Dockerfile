FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl \
  && addgroup -S proofvault \
  && adduser -S proofvault -G proofvault

WORKDIR /app

COPY --from=build /workspace/target/*.jar /app/proofvault-api.jar

USER proofvault

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
  SPRING_PROFILES_ACTIVE=dev

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/proofvault-api.jar"]
