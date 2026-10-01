"""Tuya mesaj servisini (Pulsar WebSocket) dinler ve gelen olayları çözüp yazdırır.
Kullanım: python tuya_pulsar_probe.py [saniye]   (websocket-client ve cryptography gerekir)"""
import base64, hashlib, json, sys, time
import websocket
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

props = dict(l.strip().split("=", 1) for l in open(r"C:\Users\macerce\ewelink-alarm\tuya.properties", encoding="utf-8") if "=" in l)
CID, SECRET = props["accessId"].strip(), props["accessSecret"].strip()
HOST = {"eu": "wss://mqe.tuyaeu.com:8285/"}[props.get("region", "eu").strip()]
TOPIC = sys.argv[2] if len(sys.argv) > 2 else "event"
SECONDS = int(sys.argv[1]) if len(sys.argv) > 1 else 90

md5 = lambda s: hashlib.md5(s.encode()).hexdigest()
password = md5(CID + md5(SECRET))[8:24]
url = f"{HOST}ws/v2/consumer/persistent/{CID}/out/{TOPIC}/{CID}-sub?ackTimeoutMillis=3000&subscriptionType=Failover"
key = SECRET[8:24].encode()

def decrypt(data: str, mode: str | None) -> str:
    raw = base64.b64decode(data)
    if mode and "gcm" in mode.lower():
        return AESGCM(key).decrypt(raw[:12], raw[12:], None).decode()
    d = Cipher(algorithms.AES(key), modes.ECB()).decryptor()
    out = d.update(raw) + d.finalize()
    return out[: -out[-1]].decode()

ws = websocket.create_connection(url, header={"Connection-Id": "probe", "username": CID, "password": password}, timeout=5)
print("connected", TOPIC)
end = time.time() + SECONDS
while time.time() < end:
    try:
        msg = json.loads(ws.recv())
    except websocket.WebSocketTimeoutException:
        continue
    ws.send(json.dumps({"messageId": msg["messageId"]}))
    outer = json.loads(base64.b64decode(msg["payload"]).decode())
    mode = outer.get("encryptMode") or (msg.get("properties") or {}).get("em")
    try:
        inner = json.loads(decrypt(outer["data"], mode))
    except Exception as e:  # biçim beklenenden farklıysa ham halini göster
        inner = {"decrypt_error": str(e), "outer_keys": list(outer)}
    inner.pop("localKey", None)
    print(time.strftime("%H:%M:%S"), "protocol", outer.get("protocol"), "mode", mode, json.dumps(inner, ensure_ascii=False)[:800])
ws.close()
