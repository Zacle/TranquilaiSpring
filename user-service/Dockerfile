FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
# Install parent POM so child modules can resolve it
COPY pom.xml ./pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY ai-service/pom.xml ai-service/pom.xml
COPY content-service/pom.xml content-service/pom.xml
COPY activity-service/pom.xml activity-service/pom.xml
COPY subscription-service/pom.xml subscription-service/pom.xml
COPY plan-service/pom.xml plan-service/pom.xml
COPY progress-service/pom.xml progress-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
RUN mvn install -N -q
# Build this service
COPY user-service/src user-service/src
RUN mvn package -pl user-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=builder /build/user-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseG1GC", "-jar", "app.jar"]
