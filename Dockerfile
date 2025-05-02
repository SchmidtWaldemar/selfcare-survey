FROM maven:3.9.9-eclipse-temurin-17 as build
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests

FROM eclipse-temurin:17.0.15_6-jdk
WORKDIR /app
COPY --from=build /app/target/selfcare-survey.jar /app/
EXPOSE 8081
CMD ["java", "-jar","selfcare-survey.jar"]
