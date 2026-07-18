#!/bin/sh
set -eu

mkdir -p /app/uploads/avatars
chown -R arisam:arisam /app/uploads/avatars

exec su-exec arisam "$@"
