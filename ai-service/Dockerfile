FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
RUN apk add --no-cache maven
# Install parent POM so child modules can resolve it
COPY pom.xml ./pom.xml
RUN mvn install -N -q
# Build this service
COPY ai-service/pom.xml ai-service/pom.xml
COPY ai-service/src ai-service/src
RUN mvn package -pl ai-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=builder /build/ai-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Xmx384m", "-Xms128m", "-XX:+UseG1GC", "-jar", "app.jar"]
