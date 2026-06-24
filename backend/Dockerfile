FROM gradle:9.4.1-jdk21-alpine AS build

WORKDIR /workspace
COPY . .
RUN gradle --no-daemon installDist

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S arisam && adduser -S arisam -G arisam
WORKDIR /app
COPY --from=build --chown=arisam:arisam /workspace/build/install/arisam-tunes-backend/ ./

USER arisam
EXPOSE 8080
ENTRYPOINT ["/app/bin/arisam-tunes-backend"]
