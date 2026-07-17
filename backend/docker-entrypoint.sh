#!/bin/sh
set -eu

mkdir -p /app/uploads/avatars
chown -R arisam:arisam /app/uploads/avatars

exec su -s /bin/sh arisam -c 'exec "$@"' sh "$@"
