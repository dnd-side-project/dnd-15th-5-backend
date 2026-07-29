
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew settings.gradle ./
COPY gradle gradle
COPY buildSrc buildSrc
COPY app-server app-server
COPY module-account module-account
COPY module-receipt module-receipt
COPY module-place module-place
COPY module-core module-core

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew :app-server:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV TZ=Asia/Seoul

COPY --from=build /workspace/app-server/build/libs/app-server.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
