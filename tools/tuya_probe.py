"""Tuya Cloud keşif aracı: token alır, projedeki cihazları ve veri noktalarını (DP) döker.
Gizli anahtar ve cihazın yerel anahtarı/IP/konumu yazdırılmaz."""
import hashlib, hmac, json, sys, time, uuid, urllib.request, urllib.error

props = dict(l.strip().split("=", 1) for l in open(r"C:\Users\macerce\ewelink-alarm\tuya.properties", encoding="utf-8") if "=" in l)
CID, SECRET = props["accessId"].strip(), props["accessSecret"].strip()
BASE = {"eu": "https://openapi.tuyaeu.com"}[props.get("region", "eu").strip()]
SENSITIVE = {"local_key", "localKey", "ip", "lat", "lon", "uuid", "owner_id", "ownerId", "uid", "asset_id", "bindSpaceId"}

def call(method, path, token="", body=None):
    t = str(int(time.time() * 1000)); nonce = uuid.uuid4().hex
    payload = json.dumps(body) if body is not None else ""
    content_sha = hashlib.sha256(payload.encode()).hexdigest()
    string_to_sign = f"{method}\n{content_sha}\n\n{path}"
    sign = hmac.new(SECRET.encode(), (CID + token + t + nonce + string_to_sign).encode(), hashlib.sha256).hexdigest().upper()
    headers = {"client_id": CID, "sign": sign, "t": t, "nonce": nonce, "sign_method": "HMAC-SHA256", "Content-Type": "application/json"}
    if token: headers["access_token"] = token
    req = urllib.request.Request(BASE + path, data=payload.encode() if payload else None, headers=headers, method=method)
    try:
        return json.load(urllib.request.urlopen(req, timeout=20))
    except urllib.error.HTTPError as e:
        return {"http_error": e.code, "body": e.read().decode()[:500]}

def scrub(o):
    if isinstance(o, dict): return {k: ("<gizli>" if k in SENSITIVE else scrub(v)) for k, v in o.items()}
    if isinstance(o, list): return [scrub(x) for x in o]
    return o

tok = call("GET", "/v1.0/token?grant_type=1")
if not tok.get("success"):
    print("TOKEN FAILED:", json.dumps(tok)[:400]); sys.exit(1)
token = tok["result"]["access_token"]
print("token OK, expires in", tok["result"]["expire_time"], "s")

devs = call("GET", "/v2.0/cloud/thing/device?page_size=20", token)
print("\n== device list:", json.dumps(scrub(devs), ensure_ascii=False, indent=1)[:3000])
items = devs.get("result") or []
if isinstance(items, dict): items = items.get("list", [])
for d in items:
    did = d["id"]
    print(f"\n######## {d.get('name')} | category={d.get('category')} product={d.get('product_name')} online={d.get('is_online')}")
    for label, path in [
        ("status", f"/v1.0/iot-03/devices/{did}/status"),
        ("specification", f"/v1.0/iot-03/devices/{did}/specification"),
        ("shadow", f"/v2.0/cloud/thing/{did}/shadow/properties"),
    ]:
        r = call("GET", path, token)
        print(f"-- {label}:", json.dumps(scrub(r.get("result", r)), ensure_ascii=False)[:2500])
