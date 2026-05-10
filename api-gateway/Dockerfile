FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
# Install parent POM so child modules can resolve it
COPY pom.xml ./pom.xml
RUN mvn install -N -q
# Build this service
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/src api-gateway/src
RUN mvn package -pl api-gateway -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx192m", "-Xms96m", "-XX:+UseG1GC", "-jar", "app.jar"]
