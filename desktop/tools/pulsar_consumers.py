"""Deneme B yoklayıcısı: Tuya mesaj servisini (Pulsar WebSocket) belirtilen abonelik adıyla DİNLER.
Yalnızca dinler ve mesajları onaylar (ack); cihaza hiçbir komut göndermez.

Aynı anda iki süreç başlatarak "aynı abonelik" / "farklı abonelik" davranışı karşılaştırılır:
  python pulsar_consumers.py --label A --seconds 300
  python pulsar_consumers.py --label B --seconds 300 --sub "{CID}-sub-desktop"

`{CID}` abonelik adında accessId ile değiştirilir (anahtarı komut satırına yazmamak için).
Çıktıdaki accessId, secret ve cihaz kimlikleri maskelidir.
Anahtarlar: repo kökündeki tuya.properties (gitignore'da), ya da --props.
Gereken paketler: websocket-client, cryptography."""
import argparse, base64, hashlib, html, json, re, sys, time
from pathlib import Path

import websocket
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

HOSTS = {"eu": "wss://mqe.tuyaeu.com:8285/"}
RECONNECT_WAIT = 5
MAX_REJECTIONS = 3  # el sıkışma (handshake) reddi bu kadar olunca yeniden denemeyi bırakır


def mask_id(s: str) -> str:
    return s[:3] + "***" + s[-2:] if len(s) > 8 else "***"


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--label", default="A", help="çıktıda görünen tüketici etiketi")
    ap.add_argument("--sub", default="{CID}-sub", help="abonelik adı; {CID} = accessId (vars. {CID}-sub)")
    ap.add_argument("--seconds", type=int, default=300, help="dinleme süresi (vars. 300)")
    ap.add_argument("--topic", default="event")
    ap.add_argument("--conn-id", default=None, help="Connection-Id başlığı (vars. probe-<etiket>)")
    ap.add_argument("--ping", type=int, default=30, help="WebSocket ping aralığı sn (telefon 30 sn; 0 = kapalı)")
    ap.add_argument("--props", default=str(Path(__file__).resolve().parents[2] / "tuya.properties"))
    args = ap.parse_args()

    props = dict(l.strip().split("=", 1) for l in open(args.props, encoding="utf-8") if "=" in l)
    cid, secret = props["accessId"].strip(), props["accessSecret"].strip()
    host = HOSTS[props.get("region", "eu").strip()]
    md5 = lambda s: hashlib.md5(s.encode()).hexdigest()
    password = md5(cid + md5(secret))[8:24]
    key = secret[8:24].encode()
    sub = args.sub.replace("{CID}", cid)
    url = f"{host}ws/v2/consumer/persistent/{cid}/out/{args.topic}/{sub}?ackTimeoutMillis=3000&subscriptionType=Failover"
    conn_id = args.conn_id or f"probe-{args.label}"
    label = args.label

    def scrub(text: str) -> str:
        for secret_text, shown in ((secret, "***"), (password, "***"), (cid, mask_id(cid))):
            text = text.replace(secret_text, shown)
        return text

    def say(msg: str) -> None:
        now = time.time()
        stamp = time.strftime("%H:%M:%S", time.localtime(now)) + f".{int(now * 1000) % 1000:03d}"
        print(f"{stamp} [{label}] {scrub(msg)}", flush=True)

    def decrypt(data: str, mode: str | None) -> str:
        raw = base64.b64decode(data)
        if mode and "gcm" in mode.lower():
            return AESGCM(key).decrypt(raw[:12], raw[12:], None).decode()
        d = Cipher(algorithms.AES(key), modes.ECB()).decryptor()
        out = d.update(raw) + d.finalize()
        return out[: -out[-1]].decode()

    total = connects = connect_failures = disconnects = rejections = 0
    last_ping = time.time()
    end = time.time() + args.seconds
    say(f"start sub={sub} topic={args.topic} conn-id={conn_id} seconds={args.seconds}")
    ws = None
    while time.time() < end and rejections < MAX_REJECTIONS:
        if ws is None:
            try:
                ws = websocket.create_connection(
                    url, header={"Connection-Id": conn_id, "username": cid, "password": password}, timeout=5)
                connects += 1
                say("connected (HTTP 101)")
            except websocket.WebSocketBadStatusException as e:
                connect_failures += 1
                rejections += 1
                body = getattr(e, "resp_body", b"") or b""
                if isinstance(body, bytes):
                    body = body.decode("utf-8", "replace")
                text = re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", body))).strip()  # HTML'i ayıkla
                say(f"connect REJECTED http_status={e.status_code} ({rejections}/{MAX_REJECTIONS}) "
                    f"status_line={str(e).split(' -+-+- ')[0]!r} error_text={text}")
                if rejections < MAX_REJECTIONS:
                    time.sleep(RECONNECT_WAIT)
                continue
            except Exception as e:
                connect_failures += 1
                say(f"connect FAILED {type(e).__name__}: {str(e)[:300]}")
                time.sleep(RECONNECT_WAIT)
                continue
        try:
            if args.ping and time.time() - last_ping >= args.ping:
                ws.ping()  # yalnızca WebSocket canlı tutma çerçevesi; Tuya'ya komut değildir
                last_ping = time.time()
            opcode, frame = ws.recv_data_frame(True)
            if opcode == websocket.ABNF.OPCODE_CLOSE:
                data = frame.data or b""
                raise ConnectionError(f"closed by server code={int.from_bytes(data[:2], 'big') if data else None} "
                                      f"reason={data[2:].decode('utf-8', 'replace')[:200]!r}")
            if opcode not in (websocket.ABNF.OPCODE_TEXT, websocket.ABNF.OPCODE_BINARY):
                continue  # ping/pong
            msg = json.loads(frame.data)
        except websocket.WebSocketTimeoutException:
            continue
        except Exception as e:
            disconnects += 1
            say(f"disconnected {type(e).__name__}: {str(e)[:300]}")
            try:
                ws.close()
            except Exception:
                pass
            ws = None
            time.sleep(RECONNECT_WAIT)
            continue
        if "messageId" not in msg:  # bağlantı/hata çerçevesi (ör. {"result":"failed"})
            say(f"frame {json.dumps(msg)[:300]}")
            continue
        ws.send(json.dumps({"messageId": msg["messageId"]}))  # tek gönderilen şey: ack
        total += 1
        try:
            outer = json.loads(base64.b64decode(msg["payload"]).decode())
            mode = outer.get("encryptMode") or (msg.get("properties") or {}).get("em")
            inner = json.loads(decrypt(outer["data"], mode))
            biz = inner.get("bizData") if isinstance(inner.get("bizData"), dict) else {}
            dev = inner.get("devId") or biz.get("devId") or "?"
            codes = [s.get("code") for s in inner.get("status") or []]
            first_t = ((inner.get("status") or [{}])[0] or {}).get("t")
            say(f"msg#{total} dev={mask_id(dev)} biz={inner.get('bizCode') or '-'} codes={codes} "
                f"t={first_t or inner.get('t') or '-'} dataId={inner.get('dataId') or '-'}")
        except Exception as e:
            say(f"msg#{total} decode_error {type(e).__name__}")
    if ws is not None:
        ws.close()
    say(f"TOTAL messages={total} connects={connects} connect_failures={connect_failures} disconnects={disconnects}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
