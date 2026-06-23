# Stage 1: Build the Java application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy Maven files first to optimize Docker layer caching
COPY backend-core/pom.xml ./
COPY backend-core/src ./src

# Build the Spring Boot application, skipping tests
RUN mvn clean package -DskipTests

# Stage 2: Create the production runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Python 3, pip, venv, and FFmpeg
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Set up Python virtual environment and install backend UGC dependencies
COPY ugc-engine/python_scripts/requirements.txt ./ugc-engine/python_scripts/requirements.txt
RUN python3 -m venv /opt/venv && \
    /opt/venv/bin/pip install --no-cache-dir -r ./ugc-engine/python_scripts/requirements.txt

# Prepend the virtual environment binaries to PATH so python runs inside it
ENV PATH="/opt/venv/bin:$PATH"

# Copy the compiled Java executable
COPY --from=builder /build/target/meshadacoreservice-0.0.1-SNAPSHOT.jar ./app.jar

# Copy the entire ugc-engine scripts and configs
COPY ugc-engine ./ugc-engine

# Default Spring Boot port (Railway maps incoming traffic to PORT)
EXPOSE 8080

# Execute Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
