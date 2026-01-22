# Use Java 17
FROM openjdk:17-slim

# Set working directory
WORKDIR /app

# Copy your Java files
COPY . .

# Install MySQL connector if needed
# COPY mysql-connector-j-9.5.0.jar .

# Compile Java files
RUN javac -cp ".:mysql-connector-j-9.5.0.jar" src/com/movieexplorer/*.java

# Run the server
CMD ["java", "-cp", ".:mysql-connector-j-9.5.0.jar", "com.movieexplorer.Main"]