#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""SwitchGuard masaustu uygulamasinin Windows simgesini (.ico) uretir.

Girdi : docs/brand/play-icon-512.png  (telefon uygulamasiyla ayni marka simgesi, 512x512)
Cikti : desktop/packaging/SwitchGuard.ico  (16/24/32/48/64/128/256 px)
Calistirma: python desktop/tools/make_icon.py   (gereken: Pillow)

Her boyut 512 px kaynaktan ayri ayri LANCZOS ile kucultulur; ikon dosyasina hepsi gomulur.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent.parent
SRC = ROOT / "docs" / "brand" / "play-icon-512.png"
OUT = ROOT / "desktop" / "packaging" / "SwitchGuard.ico"
SIZES = [16, 24, 32, 48, 64, 128, 256]


def main():
    base = Image.open(SRC).convert("RGBA")
    if base.size != (512, 512):
        raise SystemExit(f"{SRC} 512x512 olmali, bulunan: {base.size}")
    frames = [base.resize((s, s), Image.Resampling.LANCZOS) for s in SIZES]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    # En buyuk kare ana goruntu, digerleri append_images ile eklenir; sizes hepsini listeler.
    frames[-1].save(OUT, format="ICO", sizes=[(s, s) for s in SIZES], append_images=frames[:-1])
    print(f"yazildi: {OUT} ({OUT.stat().st_size} bayt, boyutlar: {SIZES})")


if __name__ == "__main__":
    main()
