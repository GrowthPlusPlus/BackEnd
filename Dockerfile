# 1. 빌드 단계 (Build stage)
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Gradle 설정 파일들을 먼저 복사 (캐싱 활용)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여 및 의존성 다운로드
RUN chmod +x ./gradlew
RUN ./gradlew dependencies

#윈도우 줄바꿈 문자 (/r)제거
RUN sed -i 's/\r$//' gradlew

# 소스 코드 복사 및 빌드 (테스트 제외)
COPY src src
RUN ./gradlew build -x test

# 2. 실행 단계 (Run stage)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 단계에서 생성된 jar 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# Render의 메모리 제한을 고려한 실행 명령어
ENTRYPOINT ["java", "-Xmx300m", "-jar", "app.jar"]