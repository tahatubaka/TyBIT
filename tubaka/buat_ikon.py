"""Cuts the Tubaka logo (rounded-square art on transparency) into Android icon assets.

Usage: python buat_ikon.py <source.webp> <out_dir>
Writes res-style folders under out_dir plus preview.png for eyeballing.
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
# Silhouette ramp on luminance: below LO is background (incl. its glow), above HI is glyph.
LO, HI = 28, 40
T_HEIGHT_DP = 52  # glyph height inside the 108dp adaptive layer (safe zone is 66dp)

src_path, out = Path(sys.argv[1]), Path(sys.argv[2])
rgba = np.asarray(Image.open(src_path).convert("RGBA")).astype(float)
rgb, alpha = rgba[..., :3], rgba[..., 3]

# 1. Largest centred square inside the rounded square that avoids its corners.
ys, xs = np.where(alpha > 250)
x0, y0, x1, y1 = xs.min(), ys.min(), xs.max(), ys.max()
cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
half = min(x1 - x0, y1 - y0) / 2
while alpha[int(cy - half), int(cx - half)] < 250 or alpha[int(cy + half), int(cx + half)] < 250 \
        or alpha[int(cy - half), int(cx + half)] < 250 or alpha[int(cy + half), int(cx - half)] < 250:
    half -= 2
half -= 12
box = tuple(int(v) for v in (cx - half, cy - half, cx + half, cy + half))
art = Image.fromarray(rgb.astype(np.uint8)).crop(box)
a = np.asarray(art).astype(float)

# 2. Glyph silhouette from luminance.
lum = a @ [0.299, 0.587, 0.114]
sil = np.clip((lum - LO) / (HI - LO), 0, 1)
# The background's own top highlight crosses LO too; it is whatever touches the
# crop's border (the glyph never does), so flood it away from there.
reach = Image.fromarray(((sil > 0) * 255).astype(np.uint8)).copy()  # fromarray is read-only; floodfill silently no-ops
n = reach.width
for x, y in [(i, 0) for i in range(n)] + [(i, n - 1) for i in range(n)] + [(0, i) for i in range(n)] + [(n - 1, i) for i in range(n)]:
    if reach.getpixel((x, y)):
        ImageDraw.floodfill(reach, (x, y), 0)
sil = sil * (np.asarray(reach) > 0)
sil_img = Image.fromarray((sil * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.8))
gx0, gy0, gx1, gy1 = Image.fromarray(((sil > 0.5) * 255).astype(np.uint8)).getbbox()
glyph_box = (gx0, gy0, gx1, gy1)
print("crop", box, "glyph bbox in crop", glyph_box)

ring = np.concatenate([a[:8].reshape(-1, 3), a[-8:].reshape(-1, 3), a[:, :8].reshape(-1, 3), a[:, -8:].reshape(-1, 3)])
bg = tuple(int(v) for v in np.median(ring, axis=0))
print("background", "#%02X%02X%02X" % bg)


def white(mask):
    img = Image.new("RGBA", mask.size, (255, 255, 255, 0))
    img.putalpha(mask)
    return img


def layer(size_px, mono):
    """108dp adaptive layer: art scaled so the glyph is T_HEIGHT_DP tall and centred."""
    scale = size_px * T_HEIGHT_DP / 108 / (gy1 - gy0)
    gcx, gcy = (gx0 + gx1) / 2 * scale, (gy0 + gy1) / 2 * scale
    off = (round(size_px / 2 - gcx), round(size_px / 2 - gcy))
    n = round(art.width * scale)
    if mono:
        canvas = Image.new("RGBA", (size_px, size_px), (255, 255, 255, 0))
        canvas.alpha_composite(white(sil_img.resize((n, n), Image.LANCZOS)), off)
        return canvas
    canvas = Image.new("RGBA", (size_px, size_px), bg + (255,))
    # Feather the crop's edges into the flat background so no seam shows.
    fade = Image.new("L", (n, n), 0)
    edge = round(n * 0.12)
    ImageDraw.Draw(fade).rectangle((edge, edge, n - edge, n - edge), fill=255)
    fade = fade.filter(ImageFilter.GaussianBlur(edge / 2))
    piece = art.resize((n, n), Image.LANCZOS).convert("RGBA")
    piece.putalpha(fade)
    canvas.alpha_composite(piece, off)
    return canvas


def masked(img, shape):
    m = Image.new("L", (img.width * 4, img.height * 4), 0)
    d = ImageDraw.Draw(m)
    if shape == "circle":
        d.ellipse((0, 0, m.width - 1, m.height - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, m.width - 1, m.height - 1), radius=m.width * 0.22, fill=255)
    out_img = img.copy()
    out_img.putalpha(m.resize(img.size, Image.LANCZOS))
    return out_img


def tight_silhouette(height_px, pad_px=0):
    g = sil_img.crop(glyph_box)
    w = round(g.width * height_px / g.height)
    g = g.resize((w, height_px), Image.LANCZOS)
    canvas = Image.new("L", (w + 2 * pad_px, height_px + 2 * pad_px), 0)
    canvas.paste(g, (pad_px, pad_px))
    return white(canvas)


def notif(size_px):
    inner = round(size_px * 20 / 24)
    g = tight_silhouette(inner)
    if g.width > inner:
        g = g.resize((inner, round(g.height * inner / g.width)), Image.LANCZOS)
    canvas = Image.new("RGBA", (size_px, size_px), (255, 255, 255, 0))
    canvas.alpha_composite(g, ((size_px - g.width) // 2, (size_px - g.height) // 2))
    return canvas


for d, s in DENSITIES.items():
    mip, dr = out / f"mipmap-{d}", out / f"drawable-{d}"
    mip.mkdir(parents=True, exist_ok=True)
    dr.mkdir(parents=True, exist_ok=True)
    layer(round(108 * s), mono=False).save(mip / "ic_launcher_foreground.png", optimize=True)
    layer(round(108 * s), mono=True).save(dr / "ic_launcher_monochrome.png", optimize=True)
    # Legacy 48dp icons: the full layer's visible 72dp middle.
    full = layer(round(108 * s * 4), mono=False)
    k = full.width / 108
    legacy = full.crop((round(18 * k), round(18 * k), round(90 * k), round(90 * k))).resize((round(48 * s),) * 2, Image.LANCZOS)
    masked(legacy, "square").save(mip / "ic_launcher.webp", lossless=True)
    masked(legacy, "circle").save(mip / "ic_launcher_round.webp", lossless=True)
    notif(round(24 * s)).save(dr / "ic_notification_logo.png", optimize=True)

logo = tight_silhouette(360)
logo.save(out / "ic_logo.png", optimize=True)
print("ic_logo aspect", round(logo.width / logo.height, 4), logo.size)
(out / "bg.txt").write_text("#%02X%02X%02X" % bg)

# Preview sheet: launcher as circle + squircle, themed mono on a dark tile, notif, in-app logo.
big = layer(432, mono=False)
mono = layer(432, mono=True)
sheet = Image.new("RGBA", (432 * 4 + 50, 470), (235, 235, 235, 255))
sheet.alpha_composite(masked(big.crop((72, 72, 360, 360)).resize((432, 432)), "circle"), (10, 20))
sheet.alpha_composite(masked(big.crop((72, 72, 360, 360)).resize((432, 432)), "square"), (452, 20))
tile = Image.new("RGBA", (432, 432), (40, 60, 40, 255))
tile.alpha_composite(mono)
sheet.alpha_composite(masked(tile.crop((72, 72, 360, 360)).resize((432, 432)), "circle"), (894, 20))
dark = Image.new("RGBA", (432, 432), (20, 20, 20, 255))
dark.alpha_composite(logo.resize((round(300 * logo.width / logo.height), 300)), (40, 20))
dark.alpha_composite(notif(96), (40, 330))
sheet.alpha_composite(dark, (1336, 20))
sheet.save(out / "preview.png")
