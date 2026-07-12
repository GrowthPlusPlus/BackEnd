# 1. 빌드 단계 (Build stage)
# alpine -> Debian 기반 이미지로 변경
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# 리눅스용 줄바꿈 변환 도구 설치, apk -> apt-get
RUN apt-get update && apt-get install -y dos2unix

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
# alpine -> Debian 기반 이미지
FROM eclipse-temurin:17-jre
WORKDIR /app

# =====================================================
# Playwright(Chromium)가 실행되는데 필요한 라이브러리 설치
# =====================================================
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    ca-certificates \
    fonts-liberation \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcups2 \
    libdrm2 \
    libgbm1 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libx11-xcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxkbcommon0 \
    libxrandr2 \
    xdg-utils \
    && rm -rf /var/lib/apt/lists/*

# 빌드 결과물(JAR) 복사
COPY --from=build /app/build/libs/*.jar app.jar

# =====================================================
# Playwright가 Chromium을 설치한다. 최초 docker build 때 한 번만 실행된다.
# =====================================================
RUN java -cp app.jar com.microsoft.playwright.CLI install chromium

# Render 무료 플랜 메모리 최적화 옵션 추가
ENTRYPOINT ["java", "-Xmx300m", "-Xms300m", "-jar", "app.jar"]