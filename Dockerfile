FROM eclipse-temurin:17-jdk-jammy

# Install Xvfb, x11vnc, openbox, novnc, websockify, unzip for web-based GUI streaming
RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb \
    x11vnc \
    openbox \
    novnc \
    websockify \
    net-tools \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy application libraries, sources, schema, and scripts
COPY lib/ lib/
COPY src/ src/
COPY sql/ sql/
COPY build-jar.sh build-jar.sh
COPY entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh build-jar.sh

# Build executable standalone JAR inside container using JDK 17
RUN ./build-jar.sh

# Expose noVNC web port
EXPOSE 6080

ENV DISPLAY=:0
ENV SCREEN_WIDTH=1366
ENV SCREEN_HEIGHT=768
ENV SCREEN_DEPTH=24

ENTRYPOINT ["/app/entrypoint.sh"]
