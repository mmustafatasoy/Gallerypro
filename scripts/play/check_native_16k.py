#!/usr/bin/env python3
"""Reject APK/AAB native libraries with ELF LOAD segments smaller than 16 KiB."""
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

MIN_ALIGN = 16 * 1024


def check_shared_libraries(archive):
    count = 0
    problems = []
    with zipfile.ZipFile(archive) as z:
        libs = sorted(name for name in z.namelist()
                      if name.endswith('.so') and '/lib/' in f'/{name}')
        with tempfile.TemporaryDirectory() as tmpdir:
            for index, member in enumerate(libs):
                count += 1
                f = Path(tmpdir) / f'library_{index}.so'
                f.write_bytes(z.read(member))
                report = subprocess.run(
                    ['readelf', '-lW', str(f)],
                    capture_output=True, text=True, check=True
                ).stdout
                aligns = []
                for line in report.splitlines():
                    cols = line.strip().split()
                    if cols and cols[0] == 'LOAD':
                        aligns.append(int(cols[-1], 0))
                if not aligns or any(align < MIN_ALIGN for align in aligns):
                    problems.append((member, aligns))
    if problems:
        for member, aligns in problems:
            print(f'FAIL {member}: LOAD alignment(s) = {[hex(v) for v in aligns]}')
        raise ValueError(f'{len(problems)}/{count} shared libraries are not 16 KiB aligned')
    print(f'PASS {archive}: {count} native libraries have 16 KiB ELF segment alignment')


if __name__ == '__main__':
    try:
        check_shared_libraries(sys.argv[1])
    except (OSError, ValueError, subprocess.CalledProcessError, IndexError, zipfile.BadZipFile) as e:
        sys.exit(f'ERROR: {e}')
