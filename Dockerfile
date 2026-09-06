FROM maven:3.8.8-eclipse-temurin-8 AS build

WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests clean package

FROM eclipse-temurin:8-jre-alpine

RUN addgroup -S -g 10001 ablaze && adduser -S -D -u 10001 -G ablaze ablaze
WORKDIR /app
COPY --from=build /build/target/ablaze-0.0.1-SNAPSHOT.jar /app/app.jar
RUN mkdir -p /app/logs && chown -R ablaze:ablaze /app

USER ablaze
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx384m -XX:MaxMetaspaceSize=160m -XX:MaxDirectMemorySize=32m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8181 9999

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=docker"]
