FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x ./gradlew

COPY src src

RUN ./gradlew clean bootJar --no-daemon

EXPOSE 8080

CMD ["java", "-jar", "build/libs/cloudpulse-0.0.1-SNAPSHOT.jar"]