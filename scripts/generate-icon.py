"""GalleryPro simgesi: tek tanımdan SVG + Android VectorDrawable üretir."""
import sys

def rr(x, y, w, h, r):
    x, y, w, h, r = (round(v, 2) for v in (x, y, w, h, r))
    return (f"M{x+r},{y}h{w-2*r}a{r},{r} 0 0 1 {r},{r}v{h-2*r}a{r},{r} 0 0 1 {-r},{r}"
            f"h{-(w-2*r)}a{r},{r} 0 0 1 {-r},{-r}v{-(h-2*r)}a{r},{r} 0 0 1 {r},{-r}z")

def star(cx, cy, r, k=0.0):
    # dört köşeli kıvılcım (içbükey kenarlar)
    return (f"M{cx},{cy-r}Q{cx+k},{cy-k} {cx+r},{cy}Q{cx+k},{cy+k} {cx},{cy+r}"
            f"Q{cx-k},{cy+k} {cx-r},{cy}Q{cx-k},{cy-k} {cx},{cy-r}z")

def circle(cx, cy, r):
    return f"M{cx-r},{cy}a{r},{r} 0 1 0 {2*r},0a{r},{r} 0 1 0 {-2*r},0z"

def lin(x1, y1, x2, y2, *stops):
    return dict(t="lin", x1=x1, y1=y1, x2=x2, y2=y2, stops=stops)

def rad(cx, cy, r, *stops):
    return dict(t="rad", cx=cx, cy=cy, r=r, stops=stops)

def P(d, fill=None, stroke=None, sw=0):
    return dict(k="path", d=d, fill=fill, stroke=stroke, sw=sw)

def G(children, rot=0, px=54, py=54, clip=None, tx=0, ty=0):
    return dict(k="group", children=children, rot=rot, px=px, py=py, clip=clip, tx=tx, ty=ty)

# renk: "RRGGBB" ya da ("RRGGBB", alfa)
def split(c):
    return (c, 1.0) if isinstance(c, str) else c

# ---------------- TASARIM ----------------
# Adaptif simge: 108x108 katman, başlatıcı ortadaki 72x72'yi gösterir, güvenli alan merkez (54,54) r=33.
CARD_X, CARD_Y, CARD_W, CARD_H, CARD_R = 30.5, 40, 46, 35, 7.5
CX, CY = CARD_X + CARD_W / 2, CARD_Y + CARD_H / 2

background = [
    P("M0,0h108v108h-108z", lin(18, 18, 90, 90, (0, "5A3DF6"), (0.55, "8E2DE2"), (1, "EE3F8E"))),
    P("M0,0h108v108h-108z", rad(84, 24, 40, (0, ("FFB347", 0.75)), (0.5, ("FF7A59", 0.22)), (1, ("FF7A59", 0.0)))),
    P("M0,0h108v108h-108z", rad(22, 88, 42, (0, ("2BD4F0", 0.60)), (1, ("2BD4F0", 0.0)))),
]

def glass_card(rot, tx, ty, tint, a_top, a_bot):
    return G([
        P(rr(CARD_X, CARD_Y, CARD_W, CARD_H, CARD_R),
          lin(0, CARD_Y, 0, CARD_Y + CARD_H, (0, (tint, a_top)), (1, (tint, a_bot))),
          ("FFFFFF", 0.85), 0.9),
    ], rot=rot, px=CX, py=CY, tx=tx, ty=ty)

B = 2.8  # cam çerçeve kalınlığı
ix, iy, iw, ih, ir = (round(v, 2) for v in (CARD_X + B, CARD_Y + B, CARD_W - 2 * B, CARD_H - 2 * B, CARD_R - B))
inner = rr(ix, iy, iw, ih, ir)
R_ = round(ix + iw + 1, 2); BOT = round(iy + ih + 1, 2)
SUN = (63.5, 52.5)

photo = G([
    P(inner, lin(0, iy, 0, iy + ih, (0, "3E8BFF"), (0.45, "9B8CFF"), (0.78, "FF7EB0"), (1, "FFC48A"))),
    P(circle(*SUN, 11), rad(*SUN, 11, (0, ("FFF1B8", 0.95)), (1, ("FFF1B8", 0.0)))),
    P(circle(*SUN, 5), lin(0, SUN[1] - 5, 0, SUN[1] + 5, (0, "FFFBE6"), (1, "FFC247"))),
    P(f"M{ix-1},63 L41,54 L46.5,58 L53.5,50.5 L61,58.5 L66,55.5 L{R_},61 V{BOT} H{ix-1}z",
      lin(0, 50, 0, 72, (0, "8B5CF6"), (1, "5B21B6"))),
    P(f"M{ix-1},67.5 L39,62.5 L46,66 L54,59.5 L62.5,66.5 L69.5,62.5 L{R_},66.5 V{BOT} H{ix-1}z",
      lin(0, 59, 0, 72, (0, "3730A3"), (1, "1E1B4B"))),
    # cam parlaması
    P(f"M{ix},{iy+ir}a{ir},{ir} 0 0 1 {ir},{-ir}h{iw-2*ir}a{ir},{ir} 0 0 1 {ir},{ir}v5"
      f"C{ix+iw-10},{iy+8} {ix+14},{iy+11} {ix},{iy+15}z",
      lin(0, iy, 0, iy + 15, (0, ("FFFFFF", 0.45)), (1, ("FFFFFF", 0.0)))),
], clip=inner)

foreground = [
    # yumuşak gölge
    P(f"M{CX-27},{CY+12}a27,9 0 1 0 54,0a27,9 0 1 0 -54,0z",
      rad(CX, CY + 12, 27, (0, ("14063F", 0.40)), (0.6, ("14063F", 0.15)), (1, ("14063F", 0.0)))),
    glass_card(-13, -2.5, -2.5, "7DF9FF", 0.55, 0.20),
    glass_card(10, 2.5, -3.5, "FFB3E0", 0.60, 0.22),
    # ön kart: sıvı cam çerçeve
    P(rr(CARD_X, CARD_Y, CARD_W, CARD_H, CARD_R),
      lin(0, CARD_Y, 0, CARD_Y + CARD_H, (0, ("FFFFFF", 0.97)), (1, ("E6DEFF", 0.85))),
      ("FFFFFF", 1.0), 0.5),
    photo,
    # yapay zekâ kıvılcımları
    P(star(74, 37.5, 7, 0.9), lin(0, 30.5, 0, 44.5, (0, "FFFFFF"), (1, "FFE29A"))),
    P(star(65.5, 31.5, 2.9, 0.35), ("FFFFFF", 0.95)),
]

# Tek renkli (temalı) simge
mono_back = rr(CARD_X, CARD_Y, CARD_W, CARD_H, CARD_R)
mono_paths = [
    dict(d=rr(CARD_X, CARD_Y, CARD_W, CARD_H, CARD_R) + inner, evenodd=True),
    dict(d=f"M{ix},67.5 L39,62.5 L46,66 L54,59.5 L62.5,66.5 L69.5,62.5 L{ix+iw},66.5 V{iy+ih+1} H{ix}z"),
    dict(d=circle(SUN[0], SUN[1], 4.4)),
    dict(d=star(74, 37.5, 7, 0.9)),
    dict(d=star(65.5, 31.5, 2.9, 0.35)),
]

# ---------------- ÇIKTI ----------------
def fmt(v):
    s = f"{v:.3f}".rstrip("0").rstrip(".")
    return s if s != "-0" else "0"

class SVG:
    def __init__(s): s.defs, s.n = [], 0
    def paint(s, p):
        if p is None: return "none", 1.0
        if isinstance(p, dict):
            s.n += 1; gid = f"g{s.n}"
            st = "".join(f'<stop offset="{o}" stop-color="#{split(c)[0]}" stop-opacity="{split(c)[1]}"/>' for o, c in p["stops"])
            if p["t"] == "lin":
                s.defs.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{p["x1"]}" y1="{p["y1"]}" x2="{p["x2"]}" y2="{p["y2"]}">{st}</linearGradient>')
            else:
                s.defs.append(f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{p["cx"]}" cy="{p["cy"]}" r="{p["r"]}">{st}</radialGradient>')
            return f"url(#{gid})", 1.0
        c, a = split(p); return f"#{c}", a
    def node(s, n):
        if n["k"] == "path":
            f, fa = s.paint(n["fill"]); out = f'<path d="{n["d"]}" fill="{f}" fill-opacity="{fa}"'
            if n["stroke"]:
                sc, sa = s.paint(n["stroke"]); out += f' stroke="{sc}" stroke-opacity="{sa}" stroke-width="{n["sw"]}"'
            return out + "/>"
        inner_ = "".join(s.node(c) for c in n["children"])
        tr = (f'translate({n["tx"]} {n["ty"]}) ' if (n["tx"] or n["ty"]) else "") + (f'rotate({n["rot"]} {n["px"]} {n["py"]})' if n["rot"] else "")
        attrs = f' transform="{tr.strip()}"' if tr else ""
        if n["clip"]:
            s.n += 1; cid = f"c{s.n}"
            s.defs.append(f'<clipPath id="{cid}"><path d="{n["clip"]}"/></clipPath>')
            attrs += f' clip-path="url(#{cid})"'
        return f"<g{attrs}>{inner_}</g>"
    def doc(s, layers, size=108, extra=""):
        body = "".join(s.node(n) for n in layers)
        return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 108 108">'
                f'<defs>{"".join(s.defs)}</defs>{body}{extra}</svg>')

def avd_color(c):
    c, a = split(c); return f"#{round(a*255):02X}{c}"

def avd_grad(p, attr, ind):
    stops = "".join(f'\n{ind}        <item android:offset="{o}" android:color="{avd_color(c)}" />' for o, c in p["stops"])
    if p["t"] == "lin":
        g = (f'<gradient android:type="linear" android:startX="{p["x1"]}" android:startY="{p["y1"]}" '
             f'android:endX="{p["x2"]}" android:endY="{p["y2"]}">')
    else:
        g = (f'<gradient android:type="radial" android:centerX="{p["cx"]}" android:centerY="{p["cy"]}" '
             f'android:gradientRadius="{p["r"]}">')
    return (f'\n{ind}    <aapt:attr name="{attr}">\n{ind}      {g}{stops}\n{ind}      </gradient>\n{ind}    </aapt:attr>')

def avd_node(n, ind="    "):
    if n["k"] == "path":
        a = [f'android:pathData="{n["d"]}"']; kids = ""
        if isinstance(n["fill"], dict): kids += avd_grad(n["fill"], "android:fillColor", ind)
        elif n["fill"] is not None: a.append(f'android:fillColor="{avd_color(n["fill"])}"')
        if n["stroke"]:
            a.append(f'android:strokeColor="{avd_color(n["stroke"])}"'); a.append(f'android:strokeWidth="{n["sw"]}"')
        a_s = f"\n{ind}    ".join(a)
        return f"{ind}<path\n{ind}    {a_s}" + (f">{kids}\n{ind}</path>\n" if kids else " />\n")
    a = ""
    if n["rot"]: a = f' android:rotation="{n["rot"]}" android:pivotX="{n["px"]}" android:pivotY="{n["py"]}"'
    if n["tx"] or n["ty"]: a += f' android:translateX="{n["tx"]}" android:translateY="{n["ty"]}"'
    out = f"{ind}<group{a}>\n"
    if n["clip"]: out += f'{ind}    <clip-path android:pathData="{n["clip"]}" />\n'
    out += "".join(avd_node(c, ind + "    ") for c in n["children"])
    return out + f"{ind}</group>\n"

def avd(layers, comment):
    body = "".join(avd_node(n) for n in layers)
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            f'<!-- {comment} — scripts/generate-icon.py ile üretildi; elle düzenlemeyin. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            f'{body}</vector>\n')

def avd_mono():
    out = ""
    for p in mono_paths:
        ft = '\n        android:fillType="evenOdd"' if p.get("evenodd") else ""
        out += f'    <path\n        android:fillColor="#FFFFFFFF"{ft}\n        android:pathData="{p["d"]}" />\n'
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- GalleryPro temalı (tek renk) simge — scripts/generate-icon.py ile üretildi. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            f'{out}</vector>\n')

def svg_mono():
    body = ("".join(f'<path d="{p["d"]}" fill="#fff"' + (' fill-rule="evenodd"' if p.get("evenodd") else "") + "/>" for p in mono_paths))
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><rect width="108" height="108" fill="#3a3a48"/>{body}</svg>'

def clean(t):
    import re
    return re.sub(r"-?\d+\.\d{3,}", lambda m: fmt(round(float(m.group()), 2)), t)

if __name__ == "__main__":
    # Kullanım: python3 scripts/generate-icon.py <çıktı-klasörü>
    out = sys.argv[1]
    open(f"{out}/full.svg", "w").write(clean(SVG().doc(background + foreground)))
    open(f"{out}/bg.svg", "w").write(clean(SVG().doc(background)))
    open(f"{out}/fg.svg", "w").write(clean(SVG().doc(foreground)))
    open(f"{out}/mono.svg", "w").write(clean(svg_mono()))
    open(f"{out}/ic_launcher_background.xml", "w").write(clean(avd(background, "GalleryPro simge arka planı")))
    open(f"{out}/ic_gallerypro_foreground.xml", "w").write(clean(avd(foreground, "GalleryPro simge ön planı (güvenli alan: merkez 54,54 r=33)")))
    open(f"{out}/ic_gallerypro_monochrome.xml", "w").write(clean(avd_mono()))
