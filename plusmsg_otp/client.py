"""Android OTP Relay HTTP クライアント"""

import time
from typing import Optional

import requests


class PlusMessageOTP:
    """Android端末の+Message OTP Relay アプリと通信するクライアント。"""

    def __init__(self, host: str = "192.168.1.1", port: int = 8765, timeout: float = 5.0):
        self.base_url = f"http://{host}:{port}"
        self.timeout = timeout

    def health(self) -> bool:
        """疎通確認。到達可能なら True。"""
        try:
            resp = requests.get(f"{self.base_url}/health", timeout=self.timeout)
            return resp.status_code == 200
        except requests.RequestException:
            return False

    def peek(self) -> Optional[str]:
        """現在のOTPを確認（非消費）。"""
        resp = requests.get(f"{self.base_url}/otp", timeout=self.timeout)
        if resp.status_code == 204:
            return None
        data = resp.json()
        return data.get("otp")

    def consume(self) -> Optional[str]:
        """OTPを取得して消費（1回限り）。"""
        resp = requests.post(f"{self.base_url}/otp/consume", timeout=self.timeout)
        if resp.status_code == 204:
            return None
        data = resp.json()
        return data.get("otp")

    def clear(self) -> None:
        """保持中のOTPをクリア。"""
        requests.post(f"{self.base_url}/otp/clear", timeout=self.timeout)

    def wait_for_otp(self, timeout: float = 120, poll_interval: float = 2.0) -> str:
        """OTPが届くまでポーリングし、取得＆消費して返す。

        Args:
            timeout: 最大待機秒数
            poll_interval: ポーリング間隔（秒）

        Returns:
            6桁のOTPコード

        Raises:
            TimeoutError: 指定時間内にOTPが届かなかった場合
        """
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            code = self.consume()
            if code is not None:
                return code
            time.sleep(poll_interval)
        raise TimeoutError(f"OTPが{timeout}秒以内に届きませんでした")
