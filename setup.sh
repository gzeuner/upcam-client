#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

DEST_DIR="${HOME}/upcam"

echo "[1/4] Building project (mvn clean package)..."
mvn clean package

echo "[2/4] Preparing runtime directories in ${DEST_DIR}..."
mkdir -p "${DEST_DIR}/images/received" \
         "${DEST_DIR}/images/sent" \
         "${DEST_DIR}/images/noise" \
         "${DEST_DIR}/logs" \
         "${DEST_DIR}/.state" \
         "${DEST_DIR}/.lock"

echo "[3/4] Copying runtime files..."
cp -f "./target/upcam-client-1.0-jar-with-dependencies.jar" "${DEST_DIR}/"
cp -f "./src/main/resources/application.properties" "${DEST_DIR}/"
cp -f "./src/main/resources/application.local.properties.example" "${DEST_DIR}/"
cp -f "./src/main/resources/upcamclient.properties" "${DEST_DIR}/"
cp -f "./src/main/resources/log4j2.xml" "${DEST_DIR}/"
cp -f "./upcamclient.sh" "${DEST_DIR}/"
cp -f "./upcamclient.cmd" "${DEST_DIR}/"
chmod +x "${DEST_DIR}/upcamclient.sh"

if [[ ! -f "${DEST_DIR}/application.local.properties" ]]; then
  cp -f "${DEST_DIR}/application.local.properties.example" "${DEST_DIR}/application.local.properties"
fi

echo "[4/4] Done."
echo "Runtime folder: ${DEST_DIR}"
echo "Edit ${DEST_DIR}/application.local.properties and set camera credentials."
