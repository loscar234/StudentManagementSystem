FROM eclipse-temurin:17-jdk-jammy

# Install Xvfb, x11vnc, openbox, novnc, websockify for web-based GUI streaming
RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb \
    x11vnc \
    openbox \
    novnc \
    websockify \
    net-tools \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy application libraries, distribution JAR, and scripts
COPY lib/ lib/
COPY src/ src/
COPY sql/ sql/
COPY dist/ dist/
COPY build-jar.sh build-jar.sh
COPY entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh build-jar.sh

# If dist/StudentManagementSystem.jar is missing, compile and build it
RUN if [ ! -f dist/StudentManagementSystem.jar ]; then ./build-jar.sh; fi

# Expose noVNC web port
EXPOSE 6080

ENV DISPLAY=:0
ENV SCREEN_WIDTH=1366
ENV SCREEN_HEIGHT=768
ENV SCREEN_DEPTH=24

ENTRYPOINT ["/app/entrypoint.sh"]
