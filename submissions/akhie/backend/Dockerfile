FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests 2>/dev/null || mvn package -DskipTests -o 2>/dev/null || echo "run with local mvn"
EXPOSE 8080
CMD ["java", "-jar", "target/expense-splitter-1.0.0.jar"]