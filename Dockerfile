# ===== Build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache deps
COPY pom.xml ./
RUN mvn -B -e -DskipTests dependency:go-offline

# Surse (dacă e multi-module, copiază tot proiectul)
COPY . .
# (opțional) vezi versiunile
RUN java -version && mvn -version

# dacă ai IT-uri sau teste încă pornesc, forțează skip complet:
RUN mvn -B -e -DskipTests -DskipITs -Dmaven.test.skip=true package

# ===== Runtime =====
# Dacă '18-jre-alpine' nu există în registry-ul tău, folosește '18-jre'
FROM eclipse-temurin:21-jre
WORKDIR /app
VOLUME ["/data"]
COPY --from=build /app/target/*.jar /app/app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
