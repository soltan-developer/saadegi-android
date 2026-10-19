#!/usr/bin/env bash
set -euo pipefail
# SIL OFL 1.1 font. Pin the upstream Git blob SHA, not just a moving filename.
mkdir -p app/src/main/res/font app/src/main/assets
curl -fLsS --retry 3 --connect-timeout 15   'https://raw.githubusercontent.com/google/fonts/main/ofl/vazirmatn/Vazirmatn%5Bwght%5D.ttf'   -o app/src/main/res/font/vazirmatn.ttf
expected='b02ceb054d5f1f9ac563e79248ce1bf8e35d60ca'
actual="$(git hash-object app/src/main/res/font/vazirmatn.ttf)"
test "$actual" = "$expected" || { echo 'Vazirmatn font hash mismatch'; exit 1; }
curl -fLsS --retry 3 --connect-timeout 15   'https://raw.githubusercontent.com/google/fonts/main/ofl/vazirmatn/OFL.txt'   -o app/src/main/assets/vazirmatn-OFL.txt
test "$(git hash-object app/src/main/assets/vazirmatn-OFL.txt)" = 'be66b3824e9772e75141e631695c5256f105fb57'
echo 'Vazirmatn SIL OFL font and notice prepared; hashes match pinned upstream.'
