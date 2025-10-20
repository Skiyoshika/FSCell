#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Build the plugin JAR.
mvn -q -DskipTests clean package

DIST_DIR="distribution"
DROPIN_DIR="$DIST_DIR/FSCell"
ZIP_NAME="FSCell-fiji.zip"
ZIP_PATH="$DIST_DIR/$ZIP_NAME"

rm -rf "$DROPIN_DIR"
mkdir -p "$DROPIN_DIR"

# Copy artifacts into the drop-in folder.
cp target/FSCell.jar "$DROPIN_DIR/"
cp src/main/resources/plugins.config "$DROPIN_DIR/"
cp distribution/FSCell-安装说明.txt "$DROPIN_DIR/"

# Pack the drop-in folder into a ZIP archive.
(
  cd "$DIST_DIR"
  zip -r "$ZIP_NAME" "$(basename "$DROPIN_DIR")" >/dev/null
)

echo "Created $ZIP_PATH"
