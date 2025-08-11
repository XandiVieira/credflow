# ---------- Build ----------
FROM openjdk:22-jdk-slim AS build
WORKDIR /app

# Copy wrapper & pom first for better caching
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

# Copy sources and build
COPY src ./src
RUN ./mvnw -DskipTests clean package

# ---------- Run ----------
FROM openjdk:22-jre-slim
WORKDIR /app

# Create non-root user (Debian has useradd)
RUN useradd -m -r -s /bin/false app
COPY --from=build --chown=app:app /app/target/credflow-*.jar /app/app.jar
USER app

ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"
# Spring uses ${PORT:10000}; Render sets PORT at runtime
EXPOSE 10000
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Run as non-root
RUN addgroup --system app && adduser --system --ingroup app app
USER app:app

# Memory & faster startup
ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

# Render will set PORT; your app uses ${PORT:10000}
EXPOSE 10000

CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]