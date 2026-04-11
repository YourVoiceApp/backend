# Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x ./gradlew
COPY src src
RUN ./gradlew bootJar --no-daemon -x test -Pdeploy

# Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

# bootJar는 *-plain.jar 와 실행용 JAR를 함께 만듈 수 있음
COPY --from=build /app/build/libs/ /app/libs-tmp/
RUN f="$(find /app/libs-tmp -name '*.jar' ! -name '*-plain.jar' | head -n1)" && \
    test -n "$f" && mv "$f" /app/app.jar && rm -rf /app/libs-tmp && \
    chown spring:spring /app/app.jar

USER spring:spring

EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
