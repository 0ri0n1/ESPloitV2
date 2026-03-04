#!/usr/bin/env python3
"""
Reset the ATmega32U4 on the WHID Cactus via DTR toggle.

The 32U4 enters a stale state and will not process HID commands until
it receives a DTR reset. This script opens the serial port (which
toggles DTR), waits for the chip to reboot, and verifies it's alive.

Usage:
    python cactus_reset.py                    # Reset on COM5 (default)
    python cactus_reset.py --port COM7        # Reset on COM7
    python cactus_reset.py --verify           # Reset and verify with test command
"""

import argparse
import sys
import time

try:
    import serial
except ImportError:
    print("ERROR: pyserial not installed. Run: pip install pyserial")
    sys.exit(1)

BAUD = 38400
REBOOT_WAIT = 3  # seconds


def reset_32u4(port: str, verify: bool = False) -> bool:
    """Reset the 32U4 via DTR toggle and optionally verify."""
    print(f"[*] Opening {port} at {BAUD} baud (DTR will toggle, resetting 32U4)...")

    try:
        # Do NOT set dsrdtr=True — we WANT the DTR toggle to reset the chip
        ser = serial.Serial(port, BAUD, timeout=2)
    except serial.SerialException as e:
        print(f"[-] Failed to open {port}: {e}")
        return False

    print(f"[*] Waiting {REBOOT_WAIT}s for 32U4 to reboot...")
    time.sleep(REBOOT_WAIT)

    # Drain any boot data
    if ser.in_waiting:
        ser.read(ser.in_waiting)

    if verify:
        print("[*] Verifying 32U4 responds to serial relay...")
        ser.write(b"test\n")
        time.sleep(1)
        if ser.in_waiting:
            response = ser.read(ser.in_waiting).decode("utf-8", errors="replace").strip()
            if "Relaying command" in response:
                print(f"[+] 32U4 is ALIVE: {response}")
                ser.close()
                return True
            else:
                print(f"[?] Unexpected response: {response}")
                ser.close()
                return False
        else:
            print("[-] No response from 32U4 after reset")
            ser.close()
            return False
    else:
        print("[+] 32U4 reset complete. Device should be ready for payloads.")
        ser.close()
        return True


def main():
    parser = argparse.ArgumentParser(
        description="Reset WHID Cactus ATmega32U4 via DTR toggle"
    )
    parser.add_argument(
        "--port", "-p", default="COM5", help="Serial port (default: COM5)"
    )
    parser.add_argument(
        "--verify", "-v", action="store_true",
        help="Verify 32U4 responds after reset"
    )
    args = parser.parse_args()

    success = reset_32u4(args.port, args.verify)
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
