FROM eclipse-temurin:21-jdk-alpine

COPY . .

RUN ./gradlew build -x test # Skip tests for build process.

CMD ["java", "-jar", "build/libs/app-0.0.1-SNAPSHOT.jar"]

EXPOSE 8080
