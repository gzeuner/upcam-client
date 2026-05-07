#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

CONFIG_FILE="application.properties"

java -jar upcam-client-1.0-jar-with-dependencies.jar "${CONFIG_FILE}" "log4j2.xml" "$@"
