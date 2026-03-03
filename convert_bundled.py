#!/usr/bin/env python3
"""Convert bundled_payloads.json from DuckyScript to ESPloitV2 internal format."""

import json
import re

INPUT = "android/app/src/main/assets/bundled_payloads.json"
OUTPUT = INPUT  # overwrite in place

# Key name to ESPloitV2 keycode mapping
KEY_MAP = {
    'a': 97, 'b': 98, 'c': 99, 'd': 100, 'e': 101, 'f': 102,
    'g': 103, 'h': 104, 'i': 105, 'j': 106, 'k': 107, 'l': 108,
    'm': 109, 'n': 110, 'o': 111, 'p': 112, 'q': 113, 'r': 114,
    's': 115, 't': 116, 'u': 117, 'v': 118, 'w': 119, 'x': 120,
    'y': 121, 'z': 122,
    'ENTER': 176, 'RETURN': 176,
    'SPACE': 32,
    'TAB': 179,
    'ESCAPE': 177, 'ESC': 177,
    'BACKSPACE': 178,
    'DELETE': 212, 'DEL': 212,
    'INSERT': 209,
    'HOME': 210, 'END': 211,
    'PAGEUP': 213, 'PAGEDOWN': 214,
    'UP': 218, 'UPARROW': 218,
    'DOWN': 217, 'DOWNARROW': 217,
    'LEFT': 216, 'LEFTARROW': 216,
    'RIGHT': 215, 'RIGHTARROW': 215,
    'F1': 194, 'F2': 195, 'F3': 196, 'F4': 197,
    'F5': 198, 'F6': 199, 'F7': 200, 'F8': 201,
    'F9': 202, 'F10': 203, 'F11': 204, 'F12': 205,
    'CAPSLOCK': 193, 'CAPS_LOCK': 193,
    'PRINTSCREEN': 206,
    'SCROLLLOCK': 207,
    'PAUSE': 208,
    'NUMLOCK': 219,
}

MODIFIER_MAP = {
    'GUI': 131, 'WINDOWS': 131, 'COMMAND': 131, 'META': 131,
    'CTRL': 128, 'CONTROL': 128,
    'SHIFT': 129,
    'ALT': 130,
}

def convert_ducky_line(line):
    """Convert a single DuckyScript line to ESPloitV2 internal format."""
    line = line.strip()
    if not line:
        return None

    # REM
    if line.startswith('REM '):
        return 'Rem: ' + line[4:]

    # DELAY
    m = re.match(r'^DELAY\s+(\d+)$', line)
    if m:
        return 'CustomDelay:' + m.group(1)

    # DEFAULT_DELAY / DEFAULTDELAY
    m = re.match(r'^(?:DEFAULT_?DELAY)\s+(\d+)$', line)
    if m:
        return 'DefaultDelay:' + m.group(1)

    # STRING
    if line.startswith('STRING '):
        return 'Print:' + line[7:]

    # Standalone ENTER
    if line == 'ENTER':
        return 'Press:176'

    # Standalone SPACE, TAB, ESC, etc.
    if line in KEY_MAP:
        return 'Press:' + str(KEY_MAP[line])

    # Modifier combos: GUI r, CTRL ALT t, ALT y, GUI SPACE, GUI l, etc.
    parts = line.split()
    if parts and parts[0].upper() in MODIFIER_MAP:
        codes = []
        i = 0
        while i < len(parts) and parts[i].upper() in MODIFIER_MAP:
            codes.append(MODIFIER_MAP[parts[i].upper()])
            i += 1
        if i < len(parts):
            key = parts[i]
            key_upper = key.upper()
            if key_upper in KEY_MAP:
                codes.append(KEY_MAP[key_upper])
            elif key_upper in MODIFIER_MAP:
                codes.append(MODIFIER_MAP[key_upper])
            elif len(key) == 1:
                codes.append(ord(key.lower()))
            else:
                return line  # Can't convert, pass through
        return 'Press:' + '+'.join(str(c) for c in codes)

    # Fallback: return as-is (shouldn't happen for valid DuckyScript)
    return line


def convert_payload(ducky_content):
    """Convert a full DuckyScript payload string to ESPloitV2 internal format."""
    # JSON \n becomes real newlines after json.load()
    lines = ducky_content.split('\n')
    converted = []
    for line in lines:
        result = convert_ducky_line(line)
        if result is not None:
            converted.append(result)
    # Post-process: merge Print:xxx followed by Press:176 into PrintLine:xxx
    merged = []
    i = 0
    while i < len(converted):
        if (i + 1 < len(converted)
                and converted[i].startswith('Print:')
                and converted[i + 1] == 'Press:176'):
            merged.append('PrintLine:' + converted[i][6:])
            i += 2
        else:
            merged.append(converted[i])
            i += 1
    return '\n'.join(merged)


def main():
    with open(INPUT, 'r', encoding='utf-8') as f:
        payloads = json.load(f)

    count = 0
    for payload in payloads:
        old = payload['content']
        new = convert_payload(old)
        if old != new:
            payload['content'] = new
            count += 1
            print(f"  Converted: {payload['name']}")

    with open(OUTPUT, 'w', encoding='utf-8') as f:
        json.dump(payloads, f, indent=2, ensure_ascii=False)
        f.write('\n')

    print(f"\nConverted {count}/{len(payloads)} bundled payloads to ESPloitV2 internal format")


if __name__ == '__main__':
    main()
