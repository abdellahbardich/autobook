FROM openjdk:17-slim
WORKDIR /app
COPY target/image-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]