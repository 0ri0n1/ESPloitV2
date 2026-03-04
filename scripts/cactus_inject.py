#!/usr/bin/env python3
"""
Inject an ESPloit payload into a target via the WHID Cactus WiFi API.

Sends a native ESPloit format payload to the ESP8266 web server for
immediate execution. The ESP sends commands over serial to the 32U4,
which translates them into USB HID keystrokes on the target host.

Prerequisites:
    1. Cactus plugged into target USB port
    2. 32U4 has been DTR-reset (run cactus_reset.py first)
    3. Attacker connected to Cactus WiFi AP (SSID: Exploit)

Usage:
    python cactus_inject.py payloads/hello_world.txt
    python cactus_inject.py payloads/hello_world.txt --host 192.168.1.1
    python cactus_inject.py payloads/hello_world.txt --bind 192.168.1.101
    python cactus_inject.py --live "Print:Hello World"
    python cactus_inject.py --countdown 5 payloads/hello_world.txt
"""

import argparse
import socket
import sys
import time
import urllib.parse


def send_payload(host: str, payload: str, bind_ip: str = None) -> bool:
    """Send a live payload to the ESP8266 for immediate execution."""
    form_data = urllib.parse.urlencode({
        "livepayload": payload,
        "livepayloadpresent": "1"
    }).encode()

    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(10)

        if bind_ip:
            s.bind((bind_ip, 0))

        s.connect((host, 80))

        headers = (
            f"POST /runlivepayload HTTP/1.0\r\n"
            f"Host: {host}\r\n"
            f"Content-Type: application/x-www-form-urlencoded\r\n"
            f"Content-Length: {len(form_data)}\r\n"
            f"\r\n"
        ).encode()

        s.sendall(headers + form_data)

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

        if "Running live payload" in resp_text:
            return True
        elif "200 OK" in resp_text:
            return True
        else:
            print(f"[-] Unexpected response:\n{resp_text[:500]}")
            return False

    except socket.timeout:
        print(f"[-] Connection to {host}:80 timed out")
        return False
    except OSError as e:
        print(f"[-] Network error: {e}")
        return False


def run_stored_payload(host: str, path: str, bind_ip: str = None) -> bool:
    """Trigger a stored payload on the ESP8266."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(10)

        if bind_ip:
            s.bind((bind_ip, 0))

        s.connect((host, 80))

        req = f"GET /dopayload?payload={path} HTTP/1.0\r\nHost: {host}\r\n\r\n".encode()
        s.sendall(req)

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
        return b"Running payload" in resp

    except (socket.timeout, OSError) as e:
        print(f"[-] Error: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(
        description="Inject ESPloit payload via WHID Cactus WiFi"
    )
    parser.add_argument(
        "payload_file", nargs="?",
        help="Path to payload file (native ESPloit format)"
    )
    parser.add_argument(
        "--live", "-l", type=str,
        help="Send a single-line live payload directly"
    )
    parser.add_argument(
        "--stored", "-s", type=str,
        help="Trigger a stored payload by path (e.g., /payloads/hello.txt)"
    )
    parser.add_argument(
        "--host", default="192.168.1.1",
        help="ESP8266 IP address (default: 192.168.1.1)"
    )
    parser.add_argument(
        "--bind", type=str, default=None,
        help="Local IP to bind to (for subnet conflict workaround)"
    )
    parser.add_argument(
        "--countdown", "-c", type=int, default=0,
        help="Countdown seconds before injection (gives time to switch focus)"
    )

    args = parser.parse_args()

    # Determine payload source
    payload = None
    if args.live:
        payload = args.live
    elif args.payload_file:
        try:
            with open(args.payload_file, "r") as f:
                payload = f.read().strip()
        except FileNotFoundError:
            print(f"[-] File not found: {args.payload_file}")
            sys.exit(1)
    elif args.stored:
        pass  # Handled below
    else:
        parser.print_help()
        sys.exit(1)

    # Countdown
    if args.countdown > 0:
        for i in range(args.countdown, 0, -1):
            print(f"  Injecting in {i}... (switch focus to target window)")
            time.sleep(1)

    # Execute
    if args.stored:
        print(f"[*] Triggering stored payload: {args.stored}")
        success = run_stored_payload(args.host, args.stored, args.bind)
    else:
        lines = payload.count("\n") + 1
        print(f"[*] Sending {lines}-line payload to {args.host}...")
        success = send_payload(args.host, payload, args.bind)

    if success:
        print("[+] Payload accepted and executing!")
    else:
        print("[-] Payload delivery failed")
        sys.exit(1)


if __name__ == "__main__":
    main()
