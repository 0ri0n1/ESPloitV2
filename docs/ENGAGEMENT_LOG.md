# WHID Cactus Engagement Log

Chronological record of the engagement from initial plug-in to confirmed keystroke injection.

## Session 1: Initial Setup and First Brick (2026-03-03)

### Objective
Establish C2 control over the WHID Cactus device and achieve keystroke injection.

### Actions Taken

1. **Device Identification**
   - Plugged Cactus into USB port
   - Identified as COM5, VID 1B4F:9208
   - Confirmed dual-chip architecture (ATmega32U4 + ESP-12S)

2. **Serial C2 Established**
   - Connected at 38400 baud
   - Sent `ResetDefaultConfig:` — confirmed relay to ESP
   - Built `cactus_serial_c2.py` tool for serial operations

3. **WiFi C2 Established**
   - Connected USB dongle to "Exploit" AP (DotAgency)
   - Accessed web UI at 192.168.1.1 (admin/hacktheplanet)
   - Mapped all web endpoints

4. **First Brick: SparkFun Pro Micro**
   - Attempted to compile 32U4 firmware with SparkFun Pro Micro board definition
   - Result: "Device Descriptor Request Failed" — wrong VID/PID
   - Recovery: Hall sensor double-tap + LilyPadUSB reflash

5. **Second Brick: Leonardo**
   - Same mistake with Leonardo board definition (VID 2341:8036)
   - Recovery: Same procedure — Hall sensor + LilyPadUSB

6. **ESP Firmware Flash**
   - Built `direct_flash.py` for ESP8266 flashing with DTR control
   - Flashed DuckyScript-enabled firmware via programmer sketch
   - OTA flash also tested and confirmed working

### Outcome
- Serial C2: WORKING
- WiFi C2: WORKING
- Keystroke injection: NOT WORKING (unknown at this point)

---

## Session 2: Debugging Keystroke Injection (2026-03-04)

### Objective
Diagnose why keystroke injection isn't working and fix it.

### Investigation

1. **Read Arduino_32u4_Code Source**
   - Discovered command format: `CommandName:parameters\n` (colon-delimited)
   - Commands parsed: Print, Press, PrintLine, CustomDelay, MouseMove*, MouseClick*, GetVersion
   - No startup banner (serial silence on boot is EXPECTED)
   - No handshake required
   - Serial (USB) only relays to ESP; commands come from ESP via Serial1

2. **Found Root Cause #1: DTR Reset Suppression**
   - Previous session used `dsrdtr=True` to prevent DTR toggle
   - This left the 32U4 in a stale state — it wouldn't process any commands
   - Fix: Open COM port WITHOUT `dsrdtr=True` to allow DTR reset
   - Verified: After DTR reset, 32U4 responds with "Relaying command to connected ESP device."

3. **Found Root Cause #2: DuckyScript Converter Broken**
   - Uploaded DuckyScript payload (`DELAY 1000 / GUI r / STRING notepad / ENTER`)
   - ESP said "Running payload" but nothing happened on host
   - The `ducky2esploit()` function doesn't properly translate to serial protocol
   - Fix: Use native ESPloit format exclusively

4. **Confirmed Working Pipeline**
   - Triggered `hello.txt` (native ESPloit format: `CustomDelay:3000 / Print:Hello World / Press:176`)
   - Result: "Hello World" was typed into the active window
   - The text appeared in the Claude Code chat input and was sent as a message

5. **Full Notepad Test**
   - Created native ESPloit payload: Win+R → notepad → Hello World
   - Commands: `Press:131+114 / CustomDelay:500 / Print:notepad / Press:176 / CustomDelay:1000 / Print:Hello World from ESPloit! / Press:176`
   - Sent via POST to `/runlivepayload`
   - Result: Notepad opened, "Hello World from ESPloit!" typed successfully

### Outcome
- ALL vectors WORKING
- Root causes identified and documented
- Automation scripts created
- Full POC tutorial written

---

## Key Metrics

| Metric | Value |
|--------|-------|
| Total sessions | 2 |
| Devices bricked | 2 (both recovered) |
| Root causes found | 3 (wrong board def x2, DTR suppression, DuckyScript bug) |
| Time to first brick | ~2 hours |
| Time to full injection | ~26 hours total |
| Recovery time per brick | ~30 minutes |

## Artifacts Produced

| Artifact | Purpose |
|----------|---------|
| `cactus_serial_c2.py` | Serial C2 tool |
| `direct_flash.py` | ESP8266 flash with DTR control |
| `cactus_reset.py` | 32U4 DTR reset script |
| `cactus_inject.py` | WiFi payload injection script |
| `cactus_upload.py` | Payload upload script |
| `cactus_status.py` | Device status checker |
| 6 example payloads | Hello world, whoami, PowerShell, WiFi exfil, reverse shell, lock |
| `README.md` | Full reproducible tutorial |
| `ARCHITECTURE.md` | Dual-chip system documentation |
| `OPERATIONS.md` | Quick reference guide |
| `ENGAGEMENT_LOG.md` | This file |
