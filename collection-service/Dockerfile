FROM openjdk:17-slim
WORKDIR /app
COPY target/collection-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]