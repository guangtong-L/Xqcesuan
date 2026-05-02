# ===== build stage =====
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests=true package

# ===== runtime stage =====
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
# 时区强制 Asia/Shanghai，避免跨天 bug
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata
COPY --from=build /workspace/target/fortune-backend.jar app.jar
ENV SERVER_PORT=80
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Shanghai
EXPOSE 80
ENTRYPOINT ["sh","-c","java -Xms256m -Xmx512m -Duser.timezone=Asia/Shanghai -jar app.jar"]
