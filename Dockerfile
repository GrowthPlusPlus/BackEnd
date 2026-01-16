# 1. 빌드 단계 (Build stage)
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# [추가] 리눅스용 줄바꿈 변환 도구 설치
RUN apk add --no-cache dos2unix

# 설정 파일들 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# [추가] 윈도우 줄바꿈 문자(\r)를 강제로 제거하고 실행 권한 부여
RUN dos2unix gradlew
RUN chmod +x gradlew

# 소스 복사 및 빌드 (테스트 제외)
COPY src src
RUN ./gradlew build -x test

# 2. 실행 단계 (Run stage)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 결과물(JAR) 복사
COPY --from=build /app/build/libs/*.jar app.jar

# Render 무료 플랜 메모리 최적화 옵션 추가
ENTRYPOINT ["java", "-Xmx300m", "-Xms300m", "-jar", "app.jar"]