# syntax=docker/dockerfile:1.6
FROM maven:3.9-eclipse-temurin-24 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -e -ntp dependency:go-offline
COPY src ./src
# Gera certificado autoassinado para HTTPS (desenvolvimento)
RUN keytool -genkeypair \
    -alias hyperativa \
    -keyalg RSA -keysize 2048 \
    -storetype PKCS12 \
    -keystore src/main/resources/keystore.p12 \
    -validity 3650 \
    -dname "CN=localhost,OU=API,O=Hyperativa,L=Sao Paulo,ST=SP,C=BR" \
    -storepass hyperativa123 \
    -noprompt
RUN mvn -B -e -ntp -DskipTests package

FROM eclipse-temurin:24-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring && chown spring:spring /app
USER spring
COPY --from=build /workspace/target/desafio-hyperativa.jar app.jar
EXPOSE 8080 8443
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
