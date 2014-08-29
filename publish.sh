#!/bin/bash
set -eu

OUTDIR="./dist/"
TMPOUTDIR="${TMPDIR}tomdallingcom_build/"

cd "$(dirname "$0")"
rm -Rf "$OUTDIR"
lein midje
lein build-site
echo "www.tomdalling.com" > "${OUTDIR}CNAME"
echo "Build output. See 'generator' branch for source." > "${OUTDIR}README.md"

rm -Rf "$TMPOUTDIR"
cp -R "$OUTDIR" "$TMPOUTDIR"
git checkout master
rm -Rf ./*
cp -R "${TMPOUTDIR}"* ./

gitx
