FROM eclipse-temurin:21-alpine

RUN apk update

# Install packages
RUN apk add --no-cache curl \
    ca-certificates \
    gnupg \
    perl \
    bash \
    tzdata \
    busybox-extras \ 
    && rm -rf /var/cache/apk/*

ENV TZ="Australia/Sydney"

COPY target/hl7messagingServer-1.0.0-SNAPSHOT.jar /app/hl7messagingServer-1.0.0-SNAPSHOT.jar

# Kube probes
RUN touch /tmp/healthy
RUN echo "JGroups hl7 application is running" > /tmp/healthy

ENTRYPOINT ["java", "-jar", "/app/hl7messagingServer-1.0.0-SNAPSHOT.jar"]