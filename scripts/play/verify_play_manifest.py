#!/usr/bin/env python3
"""Check the generated RELEASE manifest, never the source or debug manifest."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ANDROID_NAME = '{http://schemas.android.com/apk/res/android}name'
PERMISSION_TAGS = {'uses-permission', 'uses-permission-sdk-23', 'uses-permission-sdk-m'}
FORBIDDEN = {'android.permission.INTERNET', 'android.permission.REQUEST_INSTALL_PACKAGES'}
REQUIRED = {'android.permission.READ_MEDIA_IMAGES', 'android.permission.READ_MEDIA_VIDEO'}
EXPECTED_PACKAGE = 'com.atasoy.gallerypro'


def verify(path):
    root = ET.parse(path).getroot()
    if root.tag != 'manifest':
        raise ValueError('Manifest root tag expected')
    package = root.get('package')
    if package and package != EXPECTED_PACKAGE:
        raise ValueError(f'Unexpected package: {package}')
    permissions = {
        item.get(ANDROID_NAME)
        for item in root
        if item.tag in PERMISSION_TAGS and item.get(ANDROID_NAME)
    }
    forbidden = sorted(permissions & FORBIDDEN)
    missing = sorted(REQUIRED - permissions)
    if forbidden or missing:
        raise ValueError(f'Forbidden permissions: {forbidden}; missing gallery permissions: {missing}')
    print(f'PASS {path}: {len(permissions)} permissions, no Internet or APK install permissions')


def main():
    root = Path('app/build/intermediates')
    paths = sorted(p for p in root.rglob('AndroidManifest.xml')
                   if 'release' in p.parts
                   and ({'merged_manifest', 'merged_manifests'} & set(p.parts)))
    if not paths:
        raise ValueError('Release merged manifest not found: refusing to skip Play permission verification')
    for path in paths:
        verify(path)
    print(f'RELEASE manifest check passed ({len(paths)} file(s)).')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ET.ParseError, ValueError) as error:
        sys.exit(f'ERROR: {error}')
