#!/bin/sh
# Generate self-signed TLS certificate for Nginx Gateway (Local development & testing)
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$DIR"

echo "Generating self-signed SSL/TLS certificate for localhost..."
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout "$DIR/server.key" \
  -out "$DIR/server.crt" \
  -subj "/C=VN/ST=Hanoi/L=ThachThat/O=FPT-EMS/OU=IT/CN=localhost"

chmod 600 "$DIR/server.key"
chmod 644 "$DIR/server.crt"
echo "Certificate generated successfully at $DIR/server.crt and $DIR/server.key"

