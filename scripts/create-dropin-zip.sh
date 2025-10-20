#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Build the plugin JAR and Fiji-ready ZIP using the existing Maven assembly descriptor.
mvn -q -DskipTests clean package

# Copy the assembled ZIP into distribution/ for convenience.
mkdir -p distribution
ZIP_PATH="target/FSCell-fiji.zip"
if [[ -f "$ZIP_PATH" ]]; then
  cp "$ZIP_PATH" distribution/
  echo "Created distribution/$(basename "$ZIP_PATH")"
else
  echo "Expected $ZIP_PATH to exist but it was not generated" >&2
  exit 1
fi
