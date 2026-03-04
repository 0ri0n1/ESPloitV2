#!/usr/bin/env python3
"""
Upload a payload file to the WHID Cactus ESP8266 storage.

Uploads a native ESPloit format payload to the device's SPIFFS filesystem
so it can be executed later via the web UI or /dopayload endpoint.

Usage:
    python cactus_upload.py payloads/hello_world.txt
    python cactus_upload.py payloads/hello_world.txt --name my_payload.txt
    python cactus_upload.py payloads/hello_world.txt --bind 192.168.1.101
"""

import argparse
import os
import socket
import sys


def upload_payload(host: str, filepath: str, remote_name: str = None,
                   bind_ip: str = None) -> bool:
    """Upload a payload file to the ESP8266 SPIFFS storage."""
    if not os.path.exists(filepath):
        print(f"[-] File not found: {filepath}")
        return False

    with open(filepath, "rb") as f:
        content = f.read()

    if remote_name is None:
        remote_name = os.path.basename(filepath)

    # Build multipart form data
    boundary = "----CactusUploadBoundary"
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="upload"; '
        f'filename="/payloads/{remote_name}"\r\n'
        f"Content-Type: application/octet-stream\r\n"
        f"\r\n"
    ).encode() + content + f"\r\n--{boundary}--\r\n".encode()

    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(10)

        if bind_ip:
            s.bind((bind_ip, 0))

        s.connect((host, 80))

        headers = (
            f"POST /upload HTTP/1.0\r\n"
            f"Host: {host}\r\n"
            f"Content-Type: multipart/form-data; boundary={boundary}\r\n"
            f"Content-Length: {len(body)}\r\n"
            f"\r\n"
        ).encode()

        s.sendall(headers + body)

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

        if "200" in resp_text:
            return True
        else:
            print(f"[-] Upload response:\n{resp_text[:500]}")
            return False

    except (socket.timeout, OSError) as e:
        print(f"[-] Error: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(
        description="Upload payload to WHID Cactus storage"
    )
    parser.add_argument("file", help="Local payload file to upload")
    parser.add_argument(
        "--name", "-n", type=str, default=None,
        help="Remote filename (default: same as local)"
    )
    parser.add_argument(
        "--host", default="192.168.1.1",
        help="ESP8266 IP (default: 192.168.1.1)"
    )
    parser.add_argument(
        "--bind", type=str, default=None,
        help="Local IP to bind to"
    )

    args = parser.parse_args()

    name = args.name or os.path.basename(args.file)
    print(f"[*] Uploading {args.file} as /payloads/{name}...")
    success = upload_payload(args.host, args.file, args.name, args.bind)

    if success:
        print(f"[+] Uploaded successfully!")
        print(f"    Execute with: curl http://{args.host}/dopayload?payload=/payloads/{name}")
    else:
        print("[-] Upload failed")
        sys.exit(1)


if __name__ == "__main__":
    main()
