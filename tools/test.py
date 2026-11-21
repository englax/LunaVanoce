#!/usr/bin/env python3
"""Compile the plugin against the SDK and the Android platform, then run its tests and check the assets."""
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from android_sdk import android_platform
from plugin_assets import asset_files

root = Path(__file__).resolve().parents[1]
java_home = os.environ.get('JAVA_HOME')


def tool(name):
    return str(Path(java_home) / 'bin' / name) if java_home else name


sdk_root = Path(os.environ.get('ANDROID_HOME', os.environ.get('ANDROID_SDK_ROOT', str(Path.home() / 'android-sdk'))))
try:
    platform = str(android_platform(sdk_root))
except ValueError as error:
    raise SystemExit(str(error)) from error

sources = [*sorted((root / 'sdk/src').rglob('*.java')), *sorted((root / 'src').rglob('*.java')), *sorted((root / 'tests').rglob('*.java'))]
with tempfile.TemporaryDirectory(prefix='luna-vanoce-test-') as directory:
    subprocess.run([tool('javac'), '--release', '8', '-Xlint:-options', '-cp', platform, '-d', directory, *map(str, sources)], check=True)
    for test in ('cz.domov.luna.PocasiTest', 'cz.domov.luna.LifecycleTest'):
        subprocess.run([tool('java'), '-ea', '-cp', os.pathsep.join([directory, platform]), test], check=True)

manifest = json.loads((root / 'kiosk-satellite-plugin.json').read_text(encoding='utf-8'))
names = {name for name, _ in asset_files(root)}
for key in ('icon', 'voiceSkinThumbnail'):
    path = 'assets/' + manifest[key]
    assert path in names, f'{key} {path} is missing'
    assert (root / path).stat().st_size <= 256 * 1024, f'{key} is larger than 256 KB'
assert 'assets/advent/index.html' in names, 'the screensaver page is missing'
assert 'assets/fonts/Nunito.ttf' in names and 'assets/fonts/OFL.txt' in names, 'the font or its license is missing'
print('Assets: ok')
