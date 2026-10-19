#!/usr/bin/env bash
set -euo pipefail
# Internal-only: review full SBOM and MPL notices before release.
version="v26.10.17"
expected="654dd15d691fb151b9f8640e3041581f2b33c7b4855a15d14f33ea129d99e1e2"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
curl --fail --location --silent --show-error --retry 3 --connect-timeout 15 \
  "https://github.com/XTLS/libXray/releases/download/${version}/libxray-android.zip" \
  -o "$tmp/libxray-android.zip"
echo "${expected}  ${tmp}/libxray-android.zip" | sha256sum -c -
mkdir -p app/libs
unzip -p "$tmp/libxray-android.zip" "libxray-android/libXray.aar" > app/libs/libXray.aar
test "$(stat -c%s app/libs/libXray.aar)" -gt 5000000
echo "libXray Android AAR staged. Internal use only until license/security audit."
