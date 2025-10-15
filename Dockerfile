# === Build (Maven) ===
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline
COPY . .
RUN mvn -q -B -DskipTests package

# === Runtime (JRE) ===
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Volum pentru H2 (persistență)
VOLUME ["/data"]
# Copiază JAR-ul produs de Maven (dacă ai un nume fix, înlocuiește *.jar cu numele exact)
COPY --from=build /app/target/*.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC"
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
