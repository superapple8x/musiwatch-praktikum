#!/usr/bin/env bash
# TAMBAHAN untuk P4a Langkah 4: sertifikat lokal dengan mkcert.
# 10.0.2.2 wajib ada agar emulator Android (yang melihat laptop sebagai 10.0.2.2) memercayai sertifikat.
set -euo pipefail
mkcert -install
mkdir -p deploy/certs
mkcert -cert-file deploy/certs/api.pem -key-file deploy/certs/api-key.pem \
  api.musiwatch.test localhost 127.0.0.1 10.0.2.2
chmod 644 deploy/certs/*.pem
echo "Root CA untuk dipasang di emulator: $(mkcert -CAROOT)/rootCA.pem"
echo "Emulator: Settings > Security > Encryption & credentials > Install a certificate > CA certificate"
