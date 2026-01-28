FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy all files
COPY . .

# Check if files exist
RUN ls -la && echo "=== Java files ===" && find . -name "*.java" -type f

# Compile ALL Java files
RUN javac -cp "." $(find . -name "*.java")

# Run Main class
CMD ["java", "-cp", ".", "com.movieexplorer.Main"]