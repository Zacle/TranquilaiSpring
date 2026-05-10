FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
# Install parent POM so child modules can resolve it
COPY pom.xml ./pom.xml
RUN mvn install -N -q
# Build this service
COPY notification-service/pom.xml notification-service/pom.xml
COPY notification-service/src notification-service/src
RUN mvn package -pl notification-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=builder /build/notification-service/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseG1GC", "-jar", "app.jar"]
