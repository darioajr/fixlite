# Multi-stage build for FixLite Gateway (Standard JVM)
FROM openjdk:21-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Install Maven
RUN apt-get update && apt-get install -y wget && \
    wget https://archive.apache.org/dist/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.tar.gz && \
    tar -xzf apache-maven-3.9.4-bin.tar.gz && \
    mv apache-maven-3.9.4 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

# Build the application (standard JAR)
RUN mvn clean package -DskipTests

# Runtime stage - JVM runtime
FROM openjdk:21-jre-slim

# Create non-root user
RUN groupadd -r fixlite && \
    useradd -r -g fixlite -s /bin/false fixlite && \
    mkdir -p /app/logs && \
    chown -R fixlite:fixlite /app

# Copy the JAR file
COPY --from=builder /app/target/fixlite-gateway-1.0.0-SNAPSHOT.jar /app/fixlite-gateway.jar
COPY --from=builder /app/src/main/resources/logback.xml /app/logback.xml

# Set permissions
RUN chown fixlite:fixlite /app/fixlite-gateway.jar

# Switch to non-root user
USER fixlite

# Set working directory
WORKDIR /app

# Expose FIX port
EXPOSE 9878

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD netstat -an | grep :9878 > /dev/null; if [ 0 != $? ]; then exit 1; fi;

# Run the application
CMD ["java", "-jar", "fixlite-gateway.jar", "9878"]
