# plusmsg-otp

Android の **+メッセージ** 通知から認証コード (OTP) を Wi-Fi 経由で自動取得する Python クライアントライブラリ。

companion Android アプリが端末上で HTTP サーバーとして動作し、本ライブラリがそこから OTP をポーリングで取得します。SMS 認証付きのログインを完全自動化できます。

```
+Message 通知 → Android App (通知リスナー + HTTP Server :8765)
                     ↓ Wi-Fi LAN
                plusmsg-otp (Python クライアント)
                     ↓
                任意の認証フロー (otp_provider コールバック)
```

## インストール

```bash
pip install plusmsg-otp
```

**依存**: `requests>=2.28`　/ **Python**: 3.10+

## クイックスタート

```python
from plusmsg_otp import PlusMessageOTP

client = PlusMessageOTP(host="192.168.1.100")

# 疎通確認
client.health()  # True

# OTP を確認（消費しない）
client.peek()  # "123456" or None

# OTP を取得して消費（1回限り）
client.consume()  # "123456" or None

# OTP が届くまで待機（ポーリング）
code = client.wait_for_otp(timeout=120, poll_interval=2.0)
```

### ログイン自動化の例

```python
from plusmsg_otp import PlusMessageOTP

client = PlusMessageOTP(host="192.168.1.100")
client.clear()  # 古い OTP をクリア

# otp_provider コールバックとして渡す
token = your_auth.login(
    otp_provider=lambda: client.wait_for_otp(timeout=120),
)
```

### cron で定期実行

トークンの有効期限に合わせて cron で自動更新する例:

```cron
0 4 * * * /path/to/venv/bin/python /path/to/auto_login.py >> /path/to/cron.log 2>&1
```

## API リファレンス

### `PlusMessageOTP(host, port=8765, timeout=5.0)`

| 引数 | 型 | デフォルト | 説明 |
|------|-----|-----------|------|
| `host` | `str` | `"192.168.1.1"` | Android 端末の IP アドレス |
| `port` | `int` | `8765` | HTTP サーバーのポート番号 |
| `timeout` | `float` | `5.0` | HTTP リクエストのタイムアウト (秒) |

### メソッド

| メソッド | 説明 | 戻り値 |
|---------|------|--------|
| `health()` | 疎通確認。到達可能なら `True` | `bool` |
| `peek()` | OTP を確認（消費しない） | `str \| None` |
| `consume()` | OTP を取得して消費（1回限り） | `str \| None` |
| `clear()` | 保持中の OTP をクリア | `None` |
| `wait_for_otp(timeout, poll_interval)` | OTP が届くまでポーリング | `str` |

#### `wait_for_otp(timeout=120, poll_interval=2.0) -> str`

OTP が届くまでポーリングし、取得＆消費して返します。

- `timeout` — 最大待機秒数 (デフォルト: 120)
- `poll_interval` — ポーリング間隔秒 (デフォルト: 2.0)
- OTP が届かなかった場合は `TimeoutError` を送出

## CLI ツール

パッケージインストール時に `plusmsg-otp-watcher` コマンドが使えるようになります。
OTP の到着をリアルタイム監視するデバッグ用ツールです。

```bash
plusmsg-otp-watcher --host 192.168.1.100
# Connected. Watching for OTP...
# [04:00:15] OTP detected: 123456
```

| オプション | 説明 | デフォルト |
|-----------|------|-----------|
| `--host` | Android 端末の IP (必須) | — |
| `--port` | ポート番号 | `8765` |
| `--interval` | ポーリング間隔 (秒) | `2.0` |

## Android アプリのセットアップ

本ライブラリを使うには、Android 端末に companion アプリ (OTP Relay) をインストールする必要があります。
ソースコードはリポジトリの `android/` ディレクトリにあります。

### ビルド環境の準備

#### Java 17 (SDKMAN)

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.10-tem
```

#### Android SDK (コマンドラインツール)

Android Studio 不要。コマンドラインツールのみでビルドできます。

```bash
cd /tmp
curl -sO https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -qo commandlinetools-linux-11076708_latest.zip -d cmdline-tools-tmp
mkdir -p ~/Android/Sdk/cmdline-tools
mv cmdline-tools-tmp/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
rm -rf commandlinetools-linux-11076708_latest.zip cmdline-tools-tmp

export JAVA_HOME=$HOME/.sdkman/candidates/java/17.0.10-tem
printf 'y\n%.0s' {1..9} | \
  ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager \
    --sdk_root=~/Android/Sdk --licenses
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager \
  --sdk_root=~/Android/Sdk \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

### ビルド & インストール

```bash
cd android
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
export JAVA_HOME=$HOME/.sdkman/candidates/java/17.0.10-tem
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 通知アクセスの許可

Android 13+ ではサイドロードアプリの通知リスナー権限がブロックされます。先に制限を解除してください。

**方法A: adb コマンド (推奨)**

```bash
adb shell appops set com.example.plusmessageotp ACCESS_RESTRICTED_SETTINGS allow
```

**方法B: 設定画面から**

1. **設定 → アプリ → OTP Relay** を開く
2. 右上の **⋮** → **「制限付き設定を許可」**

制限解除後:

1. アプリを起動
2. 「通知アクセスを許可」ボタンをタップして設定画面へ遷移
3. **OTP Relay** を有効にする
4. 画面に表示される IP アドレスを控える (例: `192.168.100.56:8765`)

### 動作確認

```bash
# 疎通確認
curl http://<phone-ip>:8765/health
# {"status":"ok"}

# +メッセージでテスト送信後
curl http://<phone-ip>:8765/otp
# {"otp":"123456"}
```

## HTTP API リファレンス

Android アプリが提供する HTTP エンドポイント:

| メソッド | パス | 説明 | レスポンス例 |
|---------|------|------|-------------|
| GET | `/health` | 疎通確認 | `{"status":"ok"}` |
| GET | `/otp` | OTP 確認（非消費） | `{"otp":"123456"}` / 204 |
| POST | `/otp/consume` | OTP 取得＆消費 | `{"otp":"123456"}` / 204 |
| POST | `/otp/clear` | OTP クリア | `{"status":"cleared"}` |

- OTP 未到着時は HTTP **204 No Content** を返す
- OTP は保存から **5分** で自動失効

## 対応キャリア

| キャリア | +Message パッケージ名 |
|---------|---------------------|
| DoCoMo | `com.nttdocomo.android.msg` |
| au | `com.kddi.android.cmail` |
| SoftBank | `jp.softbank.mb.plusmessage` |

## トラブルシューティング

### 「Restricted setting」と表示されて通知アクセスを許可できない

Android 13+ のセキュリティ制限です。上記「通知アクセスの許可」の手順で制限を解除してください。

### Android アプリに接続できない

- スマホと PC が **同じ Wi-Fi** に接続されているか確認
- スマホのファイアウォールやバッテリー最適化でアプリが停止されていないか確認
- アプリを開いて IP アドレスが正しいか確認

### OTP が検出されない

- 通知アクセスが許可されているか確認 (アプリ画面でステータス表示)
- +メッセージの通知が有効か確認 (Android の通知設定)
- 認証コードが6桁の数字であることを確認

### タイムアウトする

- `wait_for_otp()` のデフォルトは 120 秒。SMS の遅延がある場合は `timeout` を伸ばす
- `poll_interval` を短くすると検出が速くなるがリクエスト数が増える

### ビルドエラー: `SDK location not found`

`android/local.properties` に `sdk.dir=/home/<user>/Android/Sdk` を記述してください。

### ビルドエラー: Java バージョン

```bash
sdk use java 17.0.10-tem
```

## License

[Unlicense](UNLICENSE) — パブリックドメイン
