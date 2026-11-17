#!/usr/bin/env python3
"""Original test scenery, never AI-mocked application UI or third-party photographs.

These images live only in the emulator and do NOT become app assets.
The Play screenshots must be captured with Android's real screencap command.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import random
import math

DEST = Path("build/store-sample-photos")
DEST.mkdir(parents=True, exist_ok=True)
S = (900, 1200)

PALETTES = [
    ((255, 194, 135), (129, 175, 209), (49, 110, 125), (34, 77, 96)),
    ((161, 210, 245), (245, 241, 212), (74, 144, 117), (27, 92, 75)),
    ((255, 198, 169), (192, 138, 174), (90, 99, 152), (42, 58, 113)),
    ((199, 216, 180), (245, 221, 170), (147, 156, 104), (72, 112, 76)),
    ((180, 205, 244), (255, 218, 189), (144, 177, 206), (67, 116, 144)),
    ((252, 221, 186), (237, 186, 166), (146, 164, 155), (74, 110, 124)),
]


def blend(a, b, k):
    return tuple(round(a[i] * (1 - k) + b[i] * k) for i in range(3))


def photo(index, palette):
    rng = random.Random(9273 + index)
    top, bottom, mountain, foreground = palette
    w, h = S
    canvas = Image.new("RGB", S)
    d = ImageDraw.Draw(canvas)
    for y in range(h):
        d.line((0, y, w, y), fill=blend(top, bottom, y / (h - 1)))
    # Soft glow, mountain skyline, naturalistic landscape textures.
    fx = int(w * (0.25 + 0.10 * index))
    fy = int(h * (0.24 + 0.01 * index))
    glow = Image.new("RGBA", S, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((fx - 90, fy - 90, fx + 90, fy + 90), fill=(255, 253, 222, 165))
    glow = glow.filter(ImageFilter.GaussianBlur(70))
    canvas = Image.alpha_composite(canvas.convert("RGBA"), glow)
    d = ImageDraw.Draw(canvas)
    for layer, color, horizon in (
        (0, mountain, 660),
        (1, blend(mountain, foreground, 0.5), 820),
        (2, foreground, 1000),
    ):
        points = [(-10, h)]
        for x in range(-10, w + 30, 24):
            wave = 90 * math.sin((x / w) * (2.8 + layer * 0.4) * math.pi + index + layer)
            jitter = rng.randint(-16, 16)
            points.append((x, horizon + int(wave) + jitter))
        points.append((w + 10, h))
        d.polygon(points, fill=(*color, 255))
    for _ in range(170):
        x = rng.randrange(w)
        y = rng.randrange(920, h)
        length = rng.randrange(3, 15)
        color = tuple(min(255, c + rng.randrange(5, 45)) for c in foreground)
        d.line((x, y, x + rng.randrange(-4, 5), y - length), fill=(*color, 80), width=1)
    canvas.convert("RGB").save(DEST / f"GalleryPro-Sample-{index+1}.jpg", quality=91, optimize=True)


for i, palette in enumerate(PALETTES):
    photo(i, palette)

print(f"Created {len(PALETTES)} original sample media files in {DEST}.")
