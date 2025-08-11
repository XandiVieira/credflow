# ---------- Build ----------
FROM amazoncorretto:22-alpine AS build
WORKDIR /app

# Tools needed by Maven Wrapper
RUN apk add --no-cache curl unzip bash

# Cache deps
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

# Build
COPY src ./src
RUN ./mvnw -DskipTests clean package

# ---------- Run ----------
FROM amazoncorretto:22-alpine
WORKDIR /app

# Non-root user
RUN addgroup -S app && adduser -S -G app app

# App
COPY --from=build --chown=app:app /app/target/credflow-*.jar /app/app.jar
USER app

# Runtime
ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"
EXPOSE 10000
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]