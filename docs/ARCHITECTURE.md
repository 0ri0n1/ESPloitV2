# WHID Cactus Architecture

## Dual-Chip Design

The WHID Cactus (Cactus Micro R2) contains two microcontrollers on a single board:

```
┌─────────────────────────────────────────────────────────┐
│                    WHID Cactus PCB                       │
│                                                          │
│  ┌──────────────┐    Serial1 @ 38400    ┌─────────────┐ │
│  │  ATmega32U4  │◄────────────────────► │  ESP-12S    │ │
│  │              │     TX1 ──► RX        │  (ESP8266)  │ │
│  │  - USB HID   │     RX1 ◄── TX        │             │ │
│  │  - Keyboard  │                       │  - WiFi AP  │ │
│  │  - Mouse     │     GPIO12 ──► GPIO0  │  - Web UI   │ │
│  │  - COM port  │     (flash mode)      │  - SPIFFS   │ │
│  │              │     GPIO13 ──► CH_PD  │  - Payloads │ │
│  │              │     (enable)          │             │ │
│  └──────┬───────┘                       └─────────────┘ │
│         │                                                │
│         │ USB                                            │
└─────────┼────────────────────────────────────────────────┘
          │
          ▼
    ┌───────────┐
    │ Target PC │  Sees: USB HID Keyboard + COM Port
    └───────────┘
```

## ATmega32U4 (USB Controller)

| Property | Value |
|----------|-------|
| Chip | ATmega32U4 |
| Clock | 8 MHz |
| Flash | 32 KB (28 KB usable) |
| SRAM | 2.5 KB |
| USB | Native USB (HID Keyboard + Mouse + CDC Serial) |
| VID:PID | 1B4F:9208 (runtime), 1B4F:9207 (bootloader) |
| Bootloader | Caterina (same as LilyPad Arduino USB) |
| Board Def | `arduino:avr:LilyPadUSB` |

**Responsibilities:**
1. Present as USB HID keyboard and mouse to the host
2. Present as USB CDC serial port (COM port) to the host
3. Relay data between USB Serial and ESP8266 Serial1
4. Parse ESPloit commands from Serial1 and translate to HID keystrokes
5. Control ESP8266 power (GPIO13 → CH_PD) and flash mode (GPIO12 → GPIO0)

**Firmware:** `Arduino_32u4_Code.ino` — a simple command parser that reads from Serial1 and calls `Keyboard.press()`, `Keyboard.print()`, `Mouse.move()`, etc.

## ESP-12S (WiFi Controller)

| Property | Value |
|----------|-------|
| Chip | ESP8266 (ESP-12S module) |
| Clock | 80 MHz |
| Flash | 4 MB (SPIFFS for payload storage) |
| WiFi | 802.11 b/g/n, AP mode |
| Default SSID | Exploit |
| Default Password | DotAgency |
| Web Server | Port 80, Basic Auth |
| OTA Port | 1337 |
| Arduino Core | **2.3.0 ONLY** (newer breaks WiFi) |

**Responsibilities:**
1. Broadcast WiFi access point for attacker connection
2. Serve web UI for payload management
3. Store payloads on SPIFFS filesystem
4. Parse and execute payload scripts
5. Send ESPloit commands to 32U4 via Serial1
6. Handle OTA firmware updates
7. Credential harvesting (ESPortal mode)

## Communication Protocol

### USB Serial (Host ↔ 32U4)
- Baud: 38400
- Purpose: Debugging, serial relay to ESP
- Direction: Bidirectional
- When host sends data → 32U4 relays to ESP via Serial1
- 32U4 prints `"Relaying command to connected ESP device."` as acknowledgment

### Internal Serial (32U4 ↔ ESP8266)
- Baud: 38400
- Hardware: 32U4 TX1/RX1 ↔ ESP RX/TX
- Purpose: Command delivery for HID injection
- Protocol: Line-based, colon-delimited (`Command:parameters\n`)

### WiFi HTTP (Attacker ↔ ESP8266)
- Port: 80 (HTTP)
- Auth: Basic (admin/hacktheplanet)
- Payload delivery: POST to `/runlivepayload`
- Stored execution: GET to `/dopayload?payload=/payloads/file.txt`

## Command Flow: Keystroke Injection

```
1. Attacker ──HTTP POST──► ESP8266 web server
   Payload: "Press:131+114\nCustomDelay:500\nPrint:notepad\nPress:176"

2. ESP8266 ──Serial1 TX──► 32U4 Serial1 RX
   Line by line: "Press:131+114\n"

3. 32U4 parses "Press" command
   Calls: Keyboard.press(131); Keyboard.press(114);
   Then:  Keyboard.releaseAll();

4. 32U4 ──USB HID──► Host PC
   Host receives: Win+R keystroke (opens Run dialog)

5. Repeat for each line of the payload
```

## Pin Mapping

| 32U4 Pin | ESP8266 Pin | Function |
|----------|-------------|----------|
| 12 (digital) | GPIO0 | Flash mode (LOW = flash, HIGH = run) |
| 13 (digital) | CH_PD/EN | Chip enable (HIGH = on, LOW = off) |
| TX1 (hardware) | RX | UART data: 32U4 → ESP |
| RX1 (hardware) | TX | UART data: ESP → 32U4 |

## Reset Mechanisms

### 32U4 Reset
- **DTR toggle:** Opening a serial port without `dsrdtr=True` toggles DTR, which resets the 32U4
- **Hall effect sensor:** Neodymium magnet near the sensor triggers reset
  - Single tap: Normal reset
  - Double tap: Enter bootloader (8-second upload window)

### ESP8266 Reset
- **Power cycle:** 32U4 controls ESP via CH_PD pin
- **Software reset:** Via web UI or serial command
- **Flash mode:** 32U4 pulls GPIO0 LOW via pin 12 during programmer sketch
