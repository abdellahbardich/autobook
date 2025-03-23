FROM openjdk:17-slim
WORKDIR /app
COPY target/pdf-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]