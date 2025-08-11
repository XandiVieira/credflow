# ---------- Build ----------
FROM eclipse-temurin:22-jdk AS build
WORKDIR /app
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src ./src
RUN ./mvnw -DskipTests clean package

# ---------- Run ----------
FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=build /app/target/credflow-*.jar /app/app.jar

RUN addgroup --system app && adduser --system --ingroup app app
USER app:app

ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"
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