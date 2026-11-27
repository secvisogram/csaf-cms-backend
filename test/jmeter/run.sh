#!/bin/bash
###############################################################################
# Run a JMeter test plan via Docker (no local JMeter install needed).
#
# Usage:
#   bash ./test/jmeter/run.sh                                  # default performance test
#   bash ./test/jmeter/run.sh test-plans/my-test.jmx           # specific test plan
#   bash ./test/jmeter/run.sh test-plans/create-advisory-performance-test.jmx -Jthreads=5 -Jloops=10
#
# Results are written to jmeter/results/<plan>-<timestamp>/
###############################################################################

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JMETER_IMAGE="${JMETER_IMAGE:-alpine/jmeter:5.6.3}"
TEST_PLAN="${1:-test-plans/create-advisory-performance-test.jmx}"
shift 2>/dev/null || true  # remaining args are passed to JMeter as extra properties

# JVM heap for JMeter — increase when testing with large CSAF files (e.g. rhsa-2018_3140.json is 32MB)
JMETER_HEAP="${JMETER_HEAP:-2g}"

# Build timestamped result directory name from the test plan filename
PLAN_NAME="$(basename "${TEST_PLAN}" .jmx)"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
RESULT_DIR="results/${PLAN_NAME}-${TIMESTAMP}"

mkdir -p "${SCRIPT_DIR}/${RESULT_DIR}"

echo "========================================"
echo " JMeter API Test Runner"
echo "========================================"
echo " Image:      ${JMETER_IMAGE}"
echo " Test plan:  ${TEST_PLAN}"
echo " Results:    jmeter/${RESULT_DIR}/"
echo " Extra args: $*"
echo "========================================"

# Verify the test plan exists
if [ ! -f "${SCRIPT_DIR}/${TEST_PLAN}" ]; then
    echo "ERROR: Test plan not found: ${SCRIPT_DIR}/${TEST_PLAN}"
    exit 1
fi

# Quick health check — warn but don't fail if backend is unreachable
if command -v curl &>/dev/null; then
    if ! curl -sf --max-time 3 http://localhost:8081/api/v1/about >/dev/null 2>&1; then
        echo ""
        echo "WARNING: Backend at http://localhost:8081 is not responding."
        echo "         Make sure 'docker compose -f docker/compose.yaml up -d' is running."
        echo ""
    fi
fi

# Convert SCRIPT_DIR to a Docker-compatible volume path.
# On Git Bash (MSYS/MinGW) the pwd is /c/Users/... but Docker Desktop needs C:\Users\...
# for the host side of -v. We convert only the host path; container paths stay as /jmeter/...
if [[ "${SCRIPT_DIR}" =~ ^/([a-zA-Z])/ ]]; then
    # Git Bash POSIX path like /c/Projects/... → C:/Projects/...
    DOCKER_HOST_DIR="${BASH_REMATCH[1]^}:${SCRIPT_DIR:2}"
else
    DOCKER_HOST_DIR="${SCRIPT_DIR}"
fi

# Run JMeter in non-GUI mode via Docker.
DOCKER_NETWORK_ARGS=()
JMETER_HOST_OVERRIDE=()

# Detect if running on Docker Desktop (Windows/Mac) vs native Linux Docker
if docker info 2>/dev/null | grep -qi "operating system.*desktop\|docker desktop"; then
    # Docker Desktop: use host.docker.internal
    DOCKER_NETWORK_ARGS+=("--add-host=host.docker.internal:host-gateway")
    JMETER_HOST_OVERRIDE+=("-Japi.host=host.docker.internal" "-Jkeycloak.host=host.docker.internal")
    echo " Docker:     Docker Desktop detected, using host.docker.internal"
else
    # Native Linux Docker: use host network mode
    DOCKER_NETWORK_ARGS+=("--network=host")
    echo " Docker:     Native Docker, using --network=host"
fi

echo ""
echo "Starting JMeter..."
echo ""

# MSYS_NO_PATHCONV=1 prevents Git Bash from mangling /jmeter/... container paths
# into C:/Program Files/Git/jmeter/... . The host volume path is already converted above.
MSYS_NO_PATHCONV=1 docker run --rm \
    "${DOCKER_NETWORK_ARGS[@]}" \
    -e JVM_ARGS="-Xms512m -Xmx${JMETER_HEAP}" \
    -v "${DOCKER_HOST_DIR}:/jmeter:rw" \
    "${JMETER_IMAGE}" \
    -n \
    -t "/jmeter/${TEST_PLAN}" \
    -l "/jmeter/${RESULT_DIR}/results.jtl" \
    -j "/jmeter/${RESULT_DIR}/jmeter.log" \
    -e -o "/jmeter/${RESULT_DIR}/report" \
    -JCookieManager.save.cookies=true \
    "${JMETER_HOST_OVERRIDE[@]}" \
    "$@"

EXIT_CODE=$?

echo ""
echo "========================================"
if [ ${EXIT_CODE} -eq 0 ]; then
    echo " JMeter finished successfully."
else
    echo " JMeter exited with code ${EXIT_CODE}."
fi
echo " Results:    jmeter/${RESULT_DIR}/results.jtl"
echo " HTML report: jmeter/${RESULT_DIR}/report/index.html"
echo " Log:        jmeter/${RESULT_DIR}/jmeter.log"
echo "========================================"

exit ${EXIT_CODE}