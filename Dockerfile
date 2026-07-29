# ---- build ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
# git에 gradlew가 100644로 들어가 있어 리눅스 러너에서 실행 권한이 없다
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon   # 의존성만 별도 레이어로 고정
COPY src src
RUN ./gradlew bootJar --no-daemon             # build 아님 → plain.jar 안 생김

# ---- runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /workspace/build/libs/app-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
