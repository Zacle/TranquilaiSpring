FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
# Install parent POM so child modules can resolve it
COPY pom.xml ./pom.xml
RUN mvn install -N -q
# Build this service
COPY progress-service/pom.xml progress-service/pom.xml
COPY progress-service/src progress-service/src
RUN mvn package -pl progress-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=builder /build/progress-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseG1GC", "-jar", "app.jar"]
