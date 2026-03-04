#!/usr/bin/env python3
"""
Check WHID Cactus device status and connectivity.

Verifies both the USB serial connection (32U4) and the WiFi connection
(ESP8266) are working. Reports device state for troubleshooting.

Usage:
    python cactus_status.py
    python cactus_status.py --port COM7
    python cactus_status.py --bind 192.168.1.101
"""

import argparse
import socket
import sys
import time

try:
    import serial
    import serial.tools.list_ports
except ImportError:
    print("ERROR: pyserial not installed. Run: pip install pyserial")
    sys.exit(1)

BAUD = 38400


def check_usb(port: str) -> dict:
    """Check 32U4 USB serial connectivity."""
    result = {"connected": False, "port": port, "vid_pid": None, "responsive": False}

    # Find the device in port list
    for p in serial.tools.list_ports.comports():
        if p.device == port:
            result["vid_pid"] = f"{p.vid:04X}:{p.pid:04X}" if p.vid else "Unknown"
            result["description"] = p.description
            result["connected"] = True
            break

    if not result["connected"]:
        return result

    # Test serial response (this will DTR-reset the 32U4)
    try:
        ser = serial.Serial(port, BAUD, timeout=2)
        time.sleep(3)  # Wait for reboot

        # Drain boot data
        if ser.in_waiting:
            ser.read(ser.in_waiting)

        # Send test
        ser.write(b"test\n")
        time.sleep(1)

        if ser.in_waiting:
            resp = ser.read(ser.in_waiting).decode("utf-8", errors="replace")
            if "Relaying command" in resp:
                result["responsive"] = True

        ser.close()
    except serial.SerialException as e:
        result["error"] = str(e)

    return result


def check_wifi(host: str, bind_ip: str = None) -> dict:
    """Check ESP8266 WiFi connectivity."""
    result = {"reachable": False, "version": None, "payloads": []}

    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(5)
        if bind_ip:
            s.bind((bind_ip, 0))
        s.connect((host, 80))

        # Request root page
        s.sendall(b"GET / HTTP/1.0\r\nHost: 192.168.1.1\r\n\r\n")
        resp = b""
        while True:
            try:
                chunk = s.recv(4096)
                if not chunk:
                    break
                resp += chunk
            except socket.timeout:
                break
        s.close()

        resp_text = resp.decode("utf-8", errors="replace")
        if "200 OK" in resp_text:
            result["reachable"] = True

        # Extract version
        if "ESPloit v" in resp_text:
            start = resp_text.index("ESPloit v") + 9
            end = resp_text.index("<", start)
            result["version"] = resp_text[start:end]

    except (socket.timeout, OSError) as e:
        result["error"] = str(e)

    # Check stored payloads
    if result["reachable"]:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(5)
            if bind_ip:
                s.bind((bind_ip, 0))
            s.connect((host, 80))
            s.sendall(b"GET /listpayloads HTTP/1.0\r\nHost: 192.168.1.1\r\n\r\n")
            resp = b""
            while True:
                try:
                    chunk = s.recv(4096)
                    if not chunk:
                        break
                    resp += chunk
                except socket.timeout:
                    break
            s.close()

            resp_text = resp.decode("utf-8", errors="replace")
            # Parse payload names from HTML
            import re
            for m in re.finditer(r'/payloads/([^"<]+)', resp_text):
                name = m.group(1)
                if name not in result["payloads"]:
                    result["payloads"].append(f"/payloads/{name}")

        except (socket.timeout, OSError):
            pass

    return result


def main():
    parser = argparse.ArgumentParser(description="Check WHID Cactus status")
    parser.add_argument("--port", "-p", default="COM5", help="Serial port")
    parser.add_argument("--host", default="192.168.1.1", help="ESP8266 IP")
    parser.add_argument("--bind", type=str, default=None, help="Local bind IP")

    args = parser.parse_args()

    print("=" * 50)
    print("  WHID Cactus Status Check")
    print("=" * 50)

    # USB check
    print("\n[USB Serial]")
    usb = check_usb(args.port)
    if usb["connected"]:
        print(f"  Port:       {usb['port']}")
        print(f"  VID:PID:    {usb['vid_pid']}")
        print(f"  Device:     {usb.get('description', 'N/A')}")
        print(f"  Responsive: {'YES' if usb['responsive'] else 'NO'}")
        if usb.get("error"):
            print(f"  Error:      {usb['error']}")
    else:
        print(f"  NOT FOUND on {args.port}")

    # WiFi check
    print("\n[WiFi / ESP8266]")
    wifi = check_wifi(args.host, args.bind)
    if wifi["reachable"]:
        print(f"  Host:       {args.host}")
        print(f"  Reachable:  YES")
        print(f"  Version:    ESPloit v{wifi['version']}" if wifi["version"] else "  Version:    Unknown")
        if wifi["payloads"]:
            print(f"  Payloads:   {len(wifi['payloads'])} stored")
            for p in wifi["payloads"]:
                print(f"              {p}")
        else:
            print(f"  Payloads:   None stored")
    else:
        print(f"  NOT REACHABLE at {args.host}")
        if wifi.get("error"):
            print(f"  Error:      {wifi['error']}")

    # Summary
    print("\n" + "=" * 50)
    if usb["responsive"] and wifi["reachable"]:
        print("  STATUS: READY FOR INJECTION")
        print("  Run: python cactus_inject.py payloads/hello_world.txt")
    elif usb["responsive"]:
        print("  STATUS: USB OK, WiFi DOWN")
        print("  Fix: Connect Wi-Fi adapter to 'Exploit' AP")
    elif wifi["reachable"]:
        print("  STATUS: WiFi OK, USB NEEDS RESET")
        print("  Fix: python cactus_reset.py")
    else:
        print("  STATUS: DEVICE NOT RESPONDING")
        print("  Check: Is the Cactus plugged in? Is WiFi adapter connected?")
    print("=" * 50)


if __name__ == "__main__":
    main()
