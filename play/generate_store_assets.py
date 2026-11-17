#!/usr/bin/env python3
"""Generate Google Play feature graphic using the existing approved GalleryPro icon.

Usage: python3 play/generate_store_assets.py
Requires Pillow (build tool only, not an Android runtime dependency).
"""
from pathlib import Path
import math
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ICON = ROOT / 'play/simge/gallerypro-play-simge-512.png'
OUTPUT = ROOT / 'play/generated/GalleryPro-feature-1024x500.png'
WIDTH, HEIGHT = 1024, 500


def font(size, bold=False):
    name = 'DejaVuSans-Bold.ttf' if bold else 'DejaVuSans.ttf'
    candidates = [
        '/usr/share/fonts/truetype/dejavu/' + name,
        '/usr/share/fonts/truetype/liberation2/' + ('LiberationSans-Bold.ttf' if bold else 'LiberationSans-Regular.ttf'),
    ]
    for candidate in candidates:
        if Path(candidate).is_file():
            return ImageFont.truetype(candidate, size)
    return ImageFont.truetype(name, size)


def background():
    canvas = Image.new('RGB', (WIDTH, HEIGHT))
    pixels = canvas.load()
    for y in range(HEIGHT):
        for x in range(WIDTH):
            gradient = x / (WIDTH - 1)
            glow1 = math.exp(-(((x - 790) / 250) ** 2 + ((y - 90) / 220) ** 2))
            glow2 = math.exp(-(((x - 170) / 320) ** 2 + ((y - 450) / 300) ** 2))
            r = min(255, int(250 - 6 * gradient + 10 * glow1))
            g = min(255, int(248 - 7 * gradient - 12 * glow1 + 3 * glow2))
            b = min(255, int(253 - 3 * gradient + 3 * glow2))
            pixels[x, y] = (r, g, b)
    return canvas


def draw_feature():
    if not ICON.is_file():
        raise FileNotFoundError('Onaylı GalleryPro simgesi bulunamadı: ' + str(ICON))
    raw_icon = Image.open(ICON)
    if raw_icon.size != (512, 512):
        raise ValueError('Google Play simgesi 512x512 olmalı: ' + str(raw_icon.size))
    icon = raw_icon.convert('RGBA').resize((274, 274), Image.Resampling.LANCZOS)

    canvas = background()
    # Glowing right-side ornament stays subtle and below the approved logo.
    overlay = Image.new('RGBA', (WIDTH, HEIGHT), (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    d.ellipse((652, 68, 1024, 438), fill=(167, 115, 234, 48))
    overlay = overlay.filter(ImageFilter.GaussianBlur(49))
    canvas = Image.alpha_composite(canvas.convert('RGBA'), overlay)

    shadow = Image.new('RGBA', (WIDTH, HEIGHT), (0, 0, 0, 0))
    d = ImageDraw.Draw(shadow)
    d.rounded_rectangle((705, 118, 979, 392), radius=60, fill=(66, 38, 95, 110))
    shadow = shadow.filter(ImageFilter.GaussianBlur(24))
    canvas = Image.alpha_composite(canvas, shadow)

    # Keep the exact icon artwork, only rounded at its outside corners.
    icon_mask = Image.new('L', (274, 274), 0)
    md = ImageDraw.Draw(icon_mask)
    md.rounded_rectangle((0, 0, 273, 273), radius=59, fill=255)
    canvas.paste(icon, (693, 106), ImageChops_multiply(icon.getchannel('A'), icon_mask))

    d = ImageDraw.Draw(canvas)
    d.text((78, 81), 'ATASOY YAZILIM', font=font(22, True), fill=(99, 76, 119, 255))
    d.text((73, 143), 'GalleryPro', font=font(73, True), fill=(37, 31, 52, 255))
    d.text((78, 263), 'Akıllı galeri. Tamamen sana özel.', font=font(27), fill=(87, 76, 103, 255))

    labels = ('Çevrimdışı arama', 'Video düzenleme', 'Gizli albüm')
    x = 78
    for label in labels:
        f = font(15, True)
        left, top, right, bottom = d.textbbox((0, 0), label, font=f)
        ww = right - left + 26
        d.rounded_rectangle((x, 337, x + ww, 380), radius=20, fill=(243, 235, 253, 255), outline=(224, 207, 246, 255), width=1)
        d.text((x + 13, 346), label, font=f, fill=(83, 56, 123, 255))
        x += ww + 12
    if x > 680:
        raise ValueError('Tanıtım etiketleri ikon bölgesine taşıyor.')
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    final = canvas.convert('RGB')
    final.save(OUTPUT, 'PNG', optimize=True)
    with Image.open(OUTPUT) as check:
        if check.size != (WIDTH, HEIGHT) or check.mode != 'RGB':
            raise ValueError('Google Play tanıtım görseli PNG/RGB 1024x500 doğrulaması başarısız.')
    if OUTPUT.stat().st_size > 15 * 1024 * 1024:
        raise ValueError('Google Play tanıtım görseli 15 MB sınırını aşıyor.')
    print(f'PASS: {OUTPUT.relative_to(ROOT)}, {OUTPUT.stat().st_size} bayt')


def ImageChops_multiply(first, second):
    from PIL import ImageChops
    return ImageChops.multiply(first, second)


if __name__ == '__main__':
    draw_feature()
