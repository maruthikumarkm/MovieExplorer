FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy source code
COPY . .

# Create output directory
RUN mkdir -p out

# Compile Java files into out/
RUN javac -d out $(find . -name "*.java")

# Debug: show compiled classes
RUN echo "=== Compiled classes ===" && find out -name "*.class"

# Run the app
CMD ["java", "-cp", "out", "com.movieexplorer.Main"]
