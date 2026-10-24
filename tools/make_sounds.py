"""Synteza dźwięków dzwonów dla Przypominajki (bez próbek – wszystko liczone od zera).

Wynik: pliki WAV (mono, 16 bit, 32 kHz) w katalogu podanym jako pierwszy argument (res/raw).
  dzwon_yeat.wav          – pojedyncze uderzenie jasnego, metalicznego dzwonu z pogłosem (styl "trap bells")
  dzwon_yeat_melodia.wav  – trzy uderzenia (melodia E6–C#6–A5) w tym samym brzmieniu
  dzwonek.wav             – miękkie "ding"
  krysztal.wav            – szklany, dwutonowy dźwięk
  rowerowy.wav            – podwójny dzwonek rowerowy
  gong.wav                – niski gong
"""
import math
import random
import struct
import sys
import wave

RATE = 32000


def silence(seconds):
    return [0.0] * int(RATE * seconds)


def add(dst, src, at_seconds, gain=1.0):
    start = int(at_seconds * RATE)
    need = start + len(src)
    if need > len(dst):
        dst.extend([0.0] * (need - len(dst)))
    for i, s in enumerate(src):
        dst[start + i] += s * gain


def partials_note(freq, seconds, partials, attack=0.003, detune_cents=0.0):
    """Suma gasnących sinusów: partials = [(stosunek, amplituda, zanik_na_sekundę), ...]."""
    n = int(RATE * seconds)
    out = [0.0] * n
    two_pi = 2 * math.pi
    for ratio, amp, decay in partials:
        f = freq * ratio
        f2 = f * (2 ** (detune_cents / 1200.0)) if detune_cents else None
        w = two_pi * f / RATE
        w2 = two_pi * f2 / RATE if f2 else None
        for i in range(n):
            t = i / RATE
            env = amp * math.exp(-decay * t)
            if env < 1e-5:
                break
            s = math.sin(w * i)
            if w2:
                s = 0.5 * (s + math.sin(w2 * i))
            out[i] += env * s
    a = int(attack * RATE)
    for i in range(min(a, n)):
        out[i] *= i / a
    return out


def fm_bell(freq, seconds, ratio=3.5, index=5.0, decay=1.6, index_decay=2.4):
    """Klasyczny dzwon FM (nośna 1 : modulator 3,5) – metaliczny, nieharmoniczny błysk."""
    n = int(RATE * seconds)
    out = [0.0] * n
    wc = 2 * math.pi * freq / RATE
    wm = 2 * math.pi * freq * ratio / RATE
    for i in range(n):
        t = i / RATE
        amp = math.exp(-decay * t)
        idx = index * math.exp(-index_decay * t)
        out[i] = amp * math.sin(wc * i + idx * math.sin(wm * i))
    a = int(0.002 * RATE)
    for i in range(a):
        out[i] *= i / a
    return out


def reverb(signal, wet=0.32, room=0.84):
    """Prosty pogłos Schroedera (4 filtry grzebieniowe + 2 wszechprzepustowe)."""
    combs = [1116, 1188, 1277, 1356]
    tail = int(RATE * 1.2)
    x = signal + [0.0] * tail
    n = len(x)
    acc = [0.0] * n
    for d in combs:
        d = int(d * RATE / 44100)
        buf = [0.0] * n
        for i in range(n):
            y = x[i] + (room * buf[i - d] if i >= d else 0.0)
            buf[i] = y
            acc[i] += y
    acc = [v / len(combs) for v in acc]
    for d, g in ((556, 0.5), (441, 0.5)):
        d = int(d * RATE / 44100)
        out = [0.0] * n
        for i in range(n):
            xd = acc[i - d] if i >= d else 0.0
            yd = out[i - d] if i >= d else 0.0
            out[i] = -g * acc[i] + xd + g * yd
        acc = out
    return [x[i] * (1 - wet) + acc[i] * wet for i in range(n)]


def tubular_hit(freq, seconds=2.0):
    """Jasny dzwon rurowy: nieharmoniczne alikwoty + warstwa FM + lekkie rozstrojenie (połysk)."""
    body = partials_note(
        freq,
        seconds,
        [(1.0, 1.0, 1.4), (2.76, 0.55, 2.2), (5.40, 0.32, 3.4), (8.93, 0.16, 5.0), (13.34, 0.07, 7.0)],
        detune_cents=6,
    )
    shine = fm_bell(freq, seconds, ratio=3.5, index=3.2, decay=2.0, index_decay=3.0)
    sub = partials_note(freq / 2, seconds, [(1.0, 0.25, 1.8)])
    return [b + 0.45 * s + u for b, s, u in zip(body, shine, sub)]


def fade_out(signal, seconds=0.25):
    n = int(seconds * RATE)
    total = len(signal)
    for i in range(max(0, total - n), total):
        signal[i] *= (total - i) / n
    return signal


def normalize(signal, peak=0.89):
    m = max(abs(s) for s in signal) or 1.0
    return [s / m * peak for s in signal]


def write(path, signal):
    signal = normalize(fade_out(signal))
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32767)) for s in signal))


def church_hit(freq, seconds=2.6):
    """Dzwon kościelny: hum (oktawa niżej), prima, tercja mała (1,2), kwinta, nominał i wyższe alikwoty,
    mocne metaliczne uderzenie na początku i długie wybrzmiewanie – mroczny, "trapowy" dzwon."""
    body = partials_note(
        freq,
        seconds,
        [(0.5, 0.55, 0.7), (1.0, 1.0, 1.1), (1.2, 0.6, 1.5), (1.5, 0.35, 1.9), (2.0, 0.7, 1.6),
         (2.5, 0.25, 2.8), (3.0, 0.2, 3.4), (4.2, 0.12, 5.0), (5.4, 0.07, 6.5)],
        attack=0.002,
        detune_cents=4,
    )
    strike = fm_bell(freq * 2, 0.4, ratio=1.41, index=6.0, decay=9.0, index_decay=12.0)
    strike += [0.0] * (len(body) - len(strike))
    return [b + 0.5 * s for b, s in zip(body, strike)]


def main(out_dir):
    random.seed(7)

    # Dzwon (C#6) – pojedyncze uderzenie z dużym pogłosem.
    write(f"{out_dir}/dzwon.wav", reverb(church_hit(1108.73, 2.4), wet=0.42, room=0.88)[: int(RATE * 2.8)])

    # Dzwon – melodia w molu: C#6, E6, C#6, G#5.
    melody = silence(0.1)
    for k, f in enumerate((1108.73, 1318.51, 1108.73, 830.61)):
        add(melody, church_hit(f, 2.2), 0.05 + k * 0.32, gain=1.0 - 0.08 * k)
    write(f"{out_dir}/dzwon_melodia.wav", reverb(melody, wet=0.4, room=0.88)[: int(RATE * 3.6)])
    # Miękkie "ding" (A5).
    ding = partials_note(880.0, 1.4, [(1.0, 1.0, 3.0), (2.0, 0.45, 4.2), (2.76, 0.28, 5.5), (4.07, 0.14, 7.5), (5.4, 0.08, 9.5)])
    write(f"{out_dir}/dzwonek.wav", ding)

    # Kryształ: G6, a po chwili D7.
    glass = silence(0.05)
    for k, f in enumerate((1567.98, 2349.32)):
        add(glass, partials_note(f, 1.3, [(1.0, 1.0, 3.6), (2.32, 0.4, 5.0), (4.25, 0.2, 7.0), (6.63, 0.08, 9.0)]), 0.02 + k * 0.13)
    write(f"{out_dir}/krysztal.wav", reverb(glass, wet=0.25)[: int(RATE * 1.8)])

    # Rowerowy: dwa szybkie "dryń" z dudnieniem.
    bike = silence(0.05)
    for k in range(2):
        add(bike, partials_note(2093.0, 0.9, [(1.0, 1.0, 4.5), (1.004, 0.9, 4.5), (2.4, 0.35, 7.0), (3.9, 0.15, 9.0)]), 0.02 + k * 0.17)
    write(f"{out_dir}/rowerowy.wav", bike[: int(RATE * 1.3)])

    # Gong: niski, długo wybrzmiewający.
    gong = partials_note(98.0, 3.2, [(1.0, 1.0, 0.9), (1.48, 0.7, 1.1), (2.0, 0.5, 1.3), (2.52, 0.4, 1.6), (3.2, 0.3, 2.0), (4.1, 0.2, 2.6), (5.3, 0.1, 3.2)], attack=0.01)
    write(f"{out_dir}/gong.wav", reverb(gong, wet=0.3)[: int(RATE * 3.4)])


if __name__ == "__main__":
    main(sys.argv[1])
