#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""SwitchGuard masaustu uygulamasinin yerlesik seslerini uretir (yalnizca standart kutuphane).

Cikti: desktop/src/main/resources/sounds/*.wav  (16 bit, mono, 44.1 kHz, PCM)
Calistirma: python desktop/tools/gen_sounds.py

Uretim tamamen deterministiktir (rastgelelik yok): ayni girdi her seferinde ayni bayt dizisini verir,
bu yuzden uretilen .wav dosyalari commit'lenir ve bu betik onlari birebir yeniden uretebilir.
"""
import math
import struct
import wave
from pathlib import Path

RATE = 44_100
PEAK = round(0.8 * 32_767)  # tam olcegin %80'i: kirpilma yok, ama yuksek
TWO_PI = 2.0 * math.pi
OUT_DIR = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "sounds"


def n_samples(seconds):
    return round(seconds * RATE)


def fade(i, n, fade_n):
    """Bastaki ve sondaki `fade_n` ornekte dogrusal yumusatma (tik sesini onler, dongude de temiz birlesir)."""
    if fade_n <= 0:
        return 1.0
    return min(1.0, i / fade_n, (n - 1 - i) / fade_n)


def siren():
    """2 s, 600 -> 1200 Hz dogrusal supurme (faz integrali alinir, bu yuzden frekans surekli)."""
    seconds, f0, f1 = 2.0, 600.0, 1200.0
    n = n_samples(seconds)
    k = (f1 - f0) / seconds
    fade_n = n_samples(0.010)
    out = []
    for i in range(n):
        t = i / RATE
        phase = TWO_PI * (f0 * t + 0.5 * k * t * t)
        out.append(math.sin(phase) * fade(i, n, fade_n))
    return out


def beep():
    """880 Hz, 3 bip (0,18 s ses + 0,12 s sessizlik) ve dongude duraklama icin 0,4 s sessizlik: toplam 1,3 s."""
    tone_n, gap_n = n_samples(0.18), n_samples(0.12)
    fade_n = n_samples(0.005)
    out = []
    for _ in range(3):
        for i in range(tone_n):
            out.append(math.sin(TWO_PI * 880.0 * i / RATE) * fade(i, tone_n, fade_n))
        out.extend([0.0] * gap_n)
    out.extend([0.0] * n_samples(0.4))
    return out


def pulse():
    """440 Hz darbeler: 4 x (0,25 s ses + 0,25 s sessizlik) = 2 s."""
    on_n, off_n = n_samples(0.25), n_samples(0.25)
    fade_n = n_samples(0.010)
    out = []
    for _ in range(4):
        for i in range(on_n):
            out.append(math.sin(TWO_PI * 440.0 * i / RATE) * fade(i, on_n, fade_n))
        out.extend([0.0] * off_n)
    return out


def chime():
    """660 + 990 Hz, 0,6 s, ustel sonumlenen bildirim cani (990 Hz daha hizli soner)."""
    n = n_samples(0.6)
    attack_n, release_n = n_samples(0.003), n_samples(0.020)
    out = []
    for i in range(n):
        t = i / RATE
        low = math.sin(TWO_PI * 660.0 * t) * math.exp(-t / 0.22)
        high = math.sin(TWO_PI * 990.0 * t) * math.exp(-t / 0.15)
        env = min(1.0, i / attack_n, (n - 1 - i) / release_n)
        out.append(0.5 * (low + high) * env)
    return out


SOUNDS = {
    "alarm_siren": siren,
    "alarm_beep": beep,
    "alarm_pulse": pulse,
    "notify_chime": chime,
}


def write_wav(path, samples):
    frames = struct.pack("<%dh" % len(samples), *(round(max(-1.0, min(1.0, s)) * PEAK) for s in samples))
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(frames)


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, make in SOUNDS.items():
        path = OUT_DIR / (name + ".wav")
        write_wav(path, make())
        print("%s  %d bayt" % (path.name, path.stat().st_size))


if __name__ == "__main__":
    main()
