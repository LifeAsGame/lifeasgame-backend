# Dockerfile (프로젝트 루트)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드된 jar 복사
COPY build/libs/*.jar app.jar

# 타임존
ENV TZ=Asia/Seoul

# curl 설치 (healthcheck용)
RUN apk add --no-cache curl

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]