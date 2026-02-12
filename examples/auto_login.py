"""iAEON 自動ログイン - +Message OTP 自動取得統合スクリプト

cron 対応のエントリーポイント。
環境変数 (aeon/.env):  PHONE_NUMBER, PASSWORD, DEVICE_ID
環境変数 (+message/.env): ANDROID_HOST, ANDROID_PORT
"""

import os
import sys
from pathlib import Path

from dotenv import load_dotenv

# aeon/.env と +message/.env を読み込む
_this_dir = Path(__file__).resolve().parent
load_dotenv(_this_dir / ".env")
load_dotenv(_this_dir.parent / "aeon" / ".env")

# iaeon_auth を import パスに追加
sys.path.insert(0, str(_this_dir.parent / "aeon"))

from iaeon_auth import IAEONAuth  # noqa: E402
from plusmsg_otp import PlusMessageOTP  # noqa: E402


def main() -> None:
    phone = os.environ["PHONE_NUMBER"]
    password = os.environ["PASSWORD"]
    device_id = os.environ.get("DEVICE_ID")
    android_host = os.environ.get("ANDROID_HOST", "192.168.1.1")
    android_port = int(os.environ.get("ANDROID_PORT", "8765"))

    otp_client = PlusMessageOTP(host=android_host, port=android_port)

    # 1. Android疎通確認
    print(f"Android端末に接続中... ({android_host}:{android_port})")
    if not otp_client.health():
        print("ERROR: Android端末に接続できません。アプリが起動しているか確認してください。")
        sys.exit(1)
    print("接続OK")

    # 2. 古いOTPクリア
    otp_client.clear()
    print("古いOTPをクリアしました")

    # 3-4. iAEONログイン（OTP自動取得）
    auth = IAEONAuth(device_id=device_id)
    token = auth.full_login(
        phone_number=phone,
        password=password,
        otp_provider=lambda: otp_client.wait_for_otp(timeout=120, poll_interval=2.0),
    )

    # 5. トークン保存
    env_path = _this_dir.parent / "aeon" / ".env"
    _update_env(env_path, "ACCESS_TOKEN", token)
    if device_id is None:
        _update_env(env_path, "DEVICE_ID", auth.device_id)
    print(f"トークンを {env_path} に保存しました")


def _update_env(env_path: Path, key: str, value: str) -> None:
    """env ファイルの指定キーを更新（なければ追記）。"""
    lines = env_path.read_text().splitlines() if env_path.exists() else []
    found = False
    for i, line in enumerate(lines):
        if line.startswith(f"{key}="):
            lines[i] = f"{key}={value}"
            found = True
            break
    if not found:
        lines.append(f"{key}={value}")
    env_path.write_text("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
