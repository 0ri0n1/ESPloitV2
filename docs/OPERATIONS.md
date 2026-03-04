# WHID Cactus Operations Reference

Quick reference for day-to-day Cactus operations. For the full tutorial, see [README.md](../README.md).

## Quick Reference Table

| Parameter | Value |
|-----------|-------|
| 32U4 USB Serial Baud | 38400 |
| 32U4 ↔ ESP8266 Baud | 38400 |
| ESP8266 Flash Baud | **115000** (not 115200) |
| avrdude Upload Baud | 57600 |
| Default SSID | Exploit |
| Default WiFi Password | DotAgency |
| Web Credentials | admin / hacktheplanet |
| Device IP | 192.168.1.1 |
| OTA Port | 1337 |
| VID:PID (runtime) | 1B4F:9208 |
| VID:PID (bootloader) | 1B4F:9207 |
| Board Definition | `arduino:avr:LilyPadUSB` |
| ESP Core Version | `esp8266:esp8266@2.3.0` |

## Standard Operating Procedure

### Before Every Session

```bash
# 1. Plug Cactus into target
# 2. Connect WiFi adapter to "Exploit" AP
# 3. Reset 32U4
python scripts/cactus_reset.py --verify

# 4. Check status
python scripts/cactus_status.py
```

### Inject Payload

```bash
# From file
python scripts/cactus_inject.py payloads/hello_world.txt

# With countdown (5s to switch focus)
python scripts/cactus_inject.py --countdown 5 payloads/hello_world.txt

# Inline command
python scripts/cactus_inject.py --live "Print:Hello World"
```

### Upload Payload for Later

```bash
python scripts/cactus_upload.py payloads/hello_world.txt
# Then trigger: curl http://192.168.1.1/dopayload?payload=/payloads/hello_world.txt
```

## Serial Commands (via USB)

These commands are typed into the USB serial port and relayed by the 32U4 to the ESP:

| Command | Effect |
|---------|--------|
| `ResetDefaultConfig:` | Factory reset (wait 15s, then replug) |
| `ResetDefaultConfig:OpenNetwork` | Reset with open AP (no password) |
| `SerialEXFIL:data` | Save data to SPIFFS |
| `GetVersion:` | Returns firmware version |
| `BlinkLED:3` | Blink LED 3 times |

## Web API Endpoints

| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/` | GET | No | Dashboard |
| `/runlivepayload` | POST | No | Execute payload immediately |
| `/dopayload?payload=PATH` | GET | No | Execute stored payload |
| `/listpayloads` | GET | No | List stored payloads |
| `/uploadpayload` | GET | No | Upload form |
| `/upload` | POST | No | File upload endpoint |
| `/showpayload?payload=PATH` | GET | No | View payload contents |
| `/deletepayload?payload=PATH` | GET | No | Delete a payload |
| `/livepayload` | GET | No | Live payload form |
| `/duckuino` | GET | No | DuckyScript converter (BROKEN) |
| `/inputmode` | GET | No | Manual keyboard/mouse input |
| `/settings` | GET | Basic | Device settings |
| `/firmware` | GET | No | Firmware update page |
| `/format` | GET | No | Format SPIFFS |
| `/exfiltrate/list` | GET | No | View exfiltrated data |

## Flashing Procedures

### Flash ESP8266 (3-Step Process)

**Only do this if you need to update ESP firmware.**

1. Flash `esp8266Programmer` to 32U4:
```bash
arduino-cli compile --fqbn "arduino:avr:LilyPadUSB" flashing/esp8266Programmer
# 1200-baud touch → upload within 8s
```

2. Flash ESP firmware:
```bash
python tools/direct_flash.py firmware.bin flash
```

3. Reflash `Arduino_32u4_Code`:
```bash
arduino-cli compile --fqbn "arduino:avr:LilyPadUSB" source/Arduino_32u4_Code
# 1200-baud touch → upload within 8s
```

4. Wait 90 seconds for SPIFFS format on first boot.

### Flash ESP8266 (OTA)

Device must be running with WiFi visible:
```bash
curl -u admin:hacktheplanet \
  -F "update=@ESP_Code.ino.generic.bin" \
  http://192.168.1.1:1337/update
```

### Recovery: ISP (Last Resort)

If bootloader is corrupted:
```bash
avrdude -c arduino -P COMx -b 19200 -p atmega32u4 -v -e \
  -U lock:w:0x3F:m -U efuse:w:0xCE:m -U hfuse:w:0xD8:m \
  -U lfuse:w:0xFF:m -U flash:w:Caterina-LilyPadUSB.hex \
  -U lock:w:0x2F:m
```

Fuses: Low=0xFF, High=0xD8, Extended=0xCE
