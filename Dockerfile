# ==============================================================================
# PalaniMart - Production 24/7 Container (100% Free Cloud Deployment)
# Compatible with Render.com, Railway, Fly.io, and Google Cloud Run
# ==============================================================================

# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B || true
COPY src ./src
COPY db ./db
RUN mvn clean package -DskipTests -B

# Runtime stage: Apache Tomcat 9 with OpenJDK 17
FROM tomcat:9.0-jdk17-temurin
LABEL maintainer="PalaniMart Team"

# Remove default Tomcat apps and deploy PalaniMart as ROOT application
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/palanimart.war /usr/local/tomcat/webapps/ROOT.war

# Set default ports and environment variables
ENV PORT=8080
ENV JDBC_URL=jdbc:h2:file:/data/palanimart;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
ENV JDBC_USER=sa
ENV JDBC_PASSWORD=

# Persistent data directory for H2 database
RUN mkdir -p /data

EXPOSE 8080

CMD ["catalina.sh", "run"]
