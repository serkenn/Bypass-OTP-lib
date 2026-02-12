"""デバッグ用の連続監視ユーティリティ。

使用例:
    python -m plusmsg_otp.watcher --host 192.168.1.100
"""

import argparse
import time

from .client import PlusMessageOTP


def main() -> None:
    parser = argparse.ArgumentParser(description="+Message OTP watcher")
    parser.add_argument("--host", required=True, help="Android端末のIPアドレス")
    parser.add_argument("--port", type=int, default=8765, help="ポート番号 (default: 8765)")
    parser.add_argument("--interval", type=float, default=2.0, help="ポーリング間隔秒 (default: 2.0)")
    args = parser.parse_args()

    client = PlusMessageOTP(host=args.host, port=args.port)

    print(f"Connecting to {client.base_url} ...")
    if not client.health():
        print("ERROR: Android端末に接続できません")
        return
    print("Connected. Watching for OTP...")

    last_otp = None
    try:
        while True:
            otp = client.peek()
            if otp != last_otp:
                if otp is not None:
                    print(f"[{time.strftime('%H:%M:%S')}] OTP detected: {otp}")
                else:
                    print(f"[{time.strftime('%H:%M:%S')}] OTP cleared")
                last_otp = otp
            time.sleep(args.interval)
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
