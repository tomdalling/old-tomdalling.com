#!/bin/bash
set -eu

MASTER_DIR="../master"
BUILD_DIR="./dist"

cd "$(dirname "$0")"
rm -Rf "$BUILD_DIR"
lein midje
lein build-site
echo "www.tomdalling.com" > "${BUILD_DIR}/CNAME"
echo "Build output. See 'generator' branch for source." > "${BUILD_DIR}/README.md"

rm -Rf "${MASTER_DIR}/"*
cp -R "${BUILD_DIR}/"* "${MASTER_DIR}/"

(cd "$MASTER_DIR" && gitup commit)
