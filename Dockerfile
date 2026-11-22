# â”€â”€ Build stage â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN apt-get update -qq && apt-get install -y -qq maven \
 && mvn package -q -DskipTests \
 && mv target/identity-manager-*.jar target/identity-manager.jar

# â”€â”€ Runtime stage â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
FROM eclipse-temurin:25-jre-noble

RUN useradd -r appuser
USER appuser

WORKDIR /app
COPY --from=build /build/target/identity-manager.jar identity-manager.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "identity-manager.jar"]