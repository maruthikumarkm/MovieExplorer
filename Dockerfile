FROM openjdk:17-slim
WORKDIR /app
COPY . .
RUN javac -cp ".:mysql-connector-j-9.5.0.jar" src/com/movieexplorer/*.java
CMD ["java", "-cp", ".:mysql-connector-j-9.5.0.jar", "com.movieexplorer.Main"]