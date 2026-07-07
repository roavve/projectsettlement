FROM eclipse-temurin:21-jre

ARG APP_UID=116
ARG APP_GID=120

RUN groupadd -g ${APP_GID} -r appuser \
 && useradd  -u ${APP_UID} -r -g appuser -d /app appuser

WORKDIR /app

RUN install -d -m 0755 -o appuser -g appuser \
      /app/export /app/fota/firmware /app/logs /app/resources /tmp

COPY --chown=appuser:appuser --chmod=0644 docu-prod-1.2.1.jar /app/app.jar
COPY --chown=appuser:appuser --chmod=0644 logback-spring.xml /app/resources/logback-spring.xml

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 TZ=Asia/Tbilisi

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-XX:InitialRAMPercentage=2", "-XX:MaxRAMPercentage=75", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/tmp/heapdump.hprof", "-Duser.timezone=Asia/Tbilisi", "-Dfile.encoding=UTF-8", "-Djava.security.egd=file:/dev/urandom", "-Dlogging.config=file:/app/resources/logback-spring.xml", "-jar","/app/app.jar"]
