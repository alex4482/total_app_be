# ===== Build (Maven) =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiem întâi pom.xml pentru cache de dependențe
COPY pom.xml ./
RUN mvn -B -e -DskipTests dependency:go-offline

# Apoi sursele (evită -q ca să vezi eroarea!)
COPY src ./src

ENV SPRING_PROFILES_ACTIVE=prod

RUN mvn -B -e -DskipTests -Dmaven.resources.encoding=UTF-8 package

# ===== Runtime (JRE) =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
VOLUME ["/data"]
COPY --from=build /app/target/*.jar /app/app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
