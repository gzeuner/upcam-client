#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

CONFIG_FILE="application.local.properties"
if [[ ! -f "${CONFIG_FILE}" ]]; then
  CONFIG_FILE="application.properties"
fi
if [[ ! -f "${CONFIG_FILE}" && -f "upcamclient.local.properties" ]]; then
  CONFIG_FILE="upcamclient.local.properties"
fi
if [[ ! -f "${CONFIG_FILE}" && -f "upcamclient.properties" ]]; then
  CONFIG_FILE="upcamclient.properties"
fi

java -jar upcam-client-1.0-jar-with-dependencies.jar "${CONFIG_FILE}" "log4j2.xml" "$@"
