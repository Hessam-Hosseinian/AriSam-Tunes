FROM gradle:9.4.1-jdk21-alpine AS build

WORKDIR /workspace
COPY . .
RUN gradle --no-daemon installDist

FROM alpine:3.20

RUN apk add --no-cache ffmpeg su-exec \
    && addgroup -S arisam \
    && adduser -S arisam -G arisam \
    && mkdir -p /app/uploads/avatars \
    && chown -R arisam:arisam /app/uploads
WORKDIR /app
COPY --from=build /opt/java/openjdk /opt/java/openjdk
COPY --from=build --chown=arisam:arisam /workspace/build/install/arisam-tunes-backend/ ./
COPY docker-entrypoint.sh /usr/local/bin/arisam-entrypoint
RUN chmod 755 /usr/local/bin/arisam-entrypoint

EXPOSE 8080
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENTRYPOINT ["arisam-entrypoint"]
CMD ["/app/bin/arisam-tunes-backend"]
