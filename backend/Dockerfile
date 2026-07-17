FROM gradle:9.4.1-jdk21-alpine AS build

WORKDIR /workspace
COPY . .
RUN gradle --no-daemon installDist

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S arisam \
    && adduser -S arisam -G arisam \
    && mkdir -p /app/uploads/avatars \
    && chown -R arisam:arisam /app/uploads
WORKDIR /app
COPY --from=build --chown=arisam:arisam /workspace/build/install/arisam-tunes-backend/ ./
COPY docker-entrypoint.sh /usr/local/bin/arisam-entrypoint
RUN chmod 755 /usr/local/bin/arisam-entrypoint

EXPOSE 8080
ENTRYPOINT ["arisam-entrypoint"]
CMD ["/app/bin/arisam-tunes-backend"]
