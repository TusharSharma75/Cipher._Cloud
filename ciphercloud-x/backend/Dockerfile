FROM maven:3.9.6-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/ciphercloud-x-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Then push to GitHub:
```
cd "C:\Users\Tushar Sharma\Downloads\CipherCloudFinal"
git add .
git commit -m "Add Dockerfile for Render"
git push origin main