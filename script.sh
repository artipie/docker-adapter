#!/bin/bash

set -e

echo "=== ENCRYPTING SETTINGS ==="
gpg --import artipie.key
rm -f ../settings.xml.asc
gpg --encrypt --sign --armor -r 646BA430 ../settings.xml
cat ../settings.xml.asc
echo "=== ENCRYPTION DONE ==="

exit 1
