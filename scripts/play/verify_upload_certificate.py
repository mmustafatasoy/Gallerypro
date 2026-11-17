#!/usr/bin/env python3
"""Compare public signer certificate fingerprints; never read/print private keys."""
import re
import subprocess
import sys

def fingerprint(bundle_path):
    check = subprocess.run(
        ['keytool', '-J-Duser.language=en', '-printcert', '-jarfile', bundle_path],
        capture_output=True, text=True, check=True,
    )
    text = check.stdout
    matches = re.findall(r'\bSHA256\s*:\s*([0-9a-fA-F:]{64,100})', text)
    if not matches:
        raise ValueError(f'Certificate SHA-256 fingerprint missing: {bundle_path}')
    if len(matches) != 1:
        raise ValueError(f'Expected exactly one signer certificate: {bundle_path}')
    digest = matches[0].replace(':', '').lower()
    if len(digest) != 64:
        raise ValueError(f'Malformed signer digest in {bundle_path}')
    if 'Android Debug' in text or 'CN=Android Debug' in text:
        raise ValueError(f'Debug signing key must never sign an upload AAB: {bundle_path}')
    return digest


def main(previous_aab, new_aab):
    old = fingerprint(previous_aab)
    new = fingerprint(new_aab)
    print(f'Existing Google Play upload certificate SHA-256: {old}')
    print(f'New release certificate SHA-256: {new}')
    if old != new:
        raise ValueError('Upload certificate differs from GalleryPro 0.9.9: DO NOT DISTRIBUTE')
    print('PASS: GalleryPro 1.0.0 uses the same existing upload certificate.')


if __name__ == '__main__':
    try:
        main(*sys.argv[1:])
    except (OSError, ValueError, subprocess.CalledProcessError, TypeError) as exc:
        sys.exit(f'ERROR: {exc}')
