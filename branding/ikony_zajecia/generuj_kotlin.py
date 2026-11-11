"""Generuje Kotlin z paczek SVG ikon SportFamily.

Użycie:
  python generuj_kotlin.py zajecia   <folder paczki 95>        <wyjście ActivityIcons.kt>
  python generuj_kotlin.py aplikacja <folder paczki brakujące> <wyjście AppIconPack.kt> [nazwa mapy]
  python generuj_kotlin.py drawable  <plik svg>                <wyjście res/drawable/*.xml>   (jednokolorowa, do powiadomień)
  python generuj_kotlin.py widzet    <folder res/drawable>     <wyjście WidgetIcons.kt> <folder paczki> [<folder paczki> ...]
                                     (widżet: linia biała do barwienia i osobno pomarańczowa kropka, pliki wi_*)

Bierze wersję svg/jasne-tlo: granat (#141B3D) to linia barwiona kolorem kategorii,
pomarańcz (#FF6A1F) to kropka, biel (#FFFFFF) to wycięcie w kolorze tła.
"""
import json, re, sys, xml.etree.ElementTree as ET

mode, src, out = sys.argv[1], sys.argv[2], sys.argv[3]
valname = sys.argv[4] if len(sys.argv) > 4 else 'AppIconPack'
NS = '{http://www.w3.org/2000/svg}'
ORANGE, NAVY, WHITE = '#FF6A1F', '#141B3D', '#FFFFFF'


def f(x):
    return ('%.3f' % x).rstrip('0').rstrip('.')


def ellipse(cx, cy, rx, ry):
    return f"M{f(cx-rx)},{f(cy)}a{f(rx)},{f(ry)} 0 1,0 {f(2*rx)},0a{f(rx)},{f(ry)} 0 1,0 {f(-2*rx)},0Z"


def rect(x, y, w, h, rx):
    if rx <= 0:
        return f"M{f(x)},{f(y)}h{f(w)}v{f(h)}h{f(-w)}Z"
    rx = min(rx, w / 2, h / 2)
    return (f"M{f(x+rx)},{f(y)}h{f(w-2*rx)}a{f(rx)},{f(rx)} 0 0 1 {f(rx)},{f(rx)}v{f(h-2*rx)}"
            f"a{f(rx)},{f(rx)} 0 0 1 {f(-rx)},{f(rx)}h{f(-(w-2*rx))}a{f(rx)},{f(rx)} 0 0 1 {f(-rx)},{f(-rx)}"
            f"v{f(-(h-2*rx))}a{f(rx)},{f(rx)} 0 0 1 {f(rx)},{f(-rx)}Z")


def parts_of(path):
    """Lista części: (d, rodzaj, grubość, obrót). Rodzaje: L linia, F wypełnienie, D kropka, C wycięcie, CS wycięcie-linia."""
    root = ET.parse(path).getroot()
    out = []

    def walk(el, inh):
        a = dict(inh)
        for k in ('fill', 'stroke', 'stroke-width'):
            if el.get(k) is not None:
                a[k] = el.get(k)
        tag = el.tag.replace(NS, '')
        n = lambda k, d=0: float(el.get(k, d))
        d = None
        if tag == 'path': d = el.get('d')
        elif tag == 'circle': d = ellipse(n('cx'), n('cy'), n('r'), n('r'))
        elif tag == 'ellipse': d = ellipse(n('cx'), n('cy'), n('rx'), n('ry'))
        elif tag == 'rect': d = rect(n('x'), n('y'), n('width'), n('height'), n('rx'))
        elif tag == 'line': d = f"M{f(n('x1'))},{f(n('y1'))}L{f(n('x2'))},{f(n('y2'))}"
        elif tag in ('polyline', 'polygon'):
            pts = el.get('points').split()
            d = 'M' + 'L'.join(pts) + ('Z' if tag == 'polygon' else '')
        elif tag not in ('g', 'title', 'svg'):
            raise SystemExit(f'Nieobsługiwany element {tag} w {path}')
        if d:
            rot = None
            m = re.match(r'rotate\(([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\)', el.get('transform') or '')
            if m: rot = tuple(float(x) for x in m.groups())
            elif el.get('transform'): raise SystemExit(f'Nieobsługiwane przekształcenie w {path}')
            fill, stroke = a.get('fill', 'none').upper(), a.get('stroke', 'none').upper()
            w = float(a.get('stroke-width', '2'))
            if fill == ORANGE: out.append((d, 'D', 0, rot))
            elif fill == NAVY: out.append((d, 'F', 0, rot))
            elif fill == WHITE: out.append((d, 'C', 0, rot))
            if stroke == NAVY: out.append((d, 'L', w, rot))
            elif stroke == WHITE: out.append((d, 'CS', w, rot))
        for c in el:
            walk(c, a)

    for c in root:
        walk(c, {})
    return out


def kt_parts(parts, indent):
    rows = []
    for d, kind, w, rot in parts:
        extra = f', w = {f(w)}f' if kind in ('L', 'CS') and w != 2 else ''
        if rot: extra += f', rot = {f(rot[0])}f, px = {f(rot[1])}f, py = {f(rot[2])}f'
        rows.append(f'{indent}P("{d}", K.{kind}{extra}),')
    return '\n'.join(rows)


def vector_xml(parts, name, dot):
    rows = []
    for d, kind, w, rot in parts:
        if kind in ('C', 'CS'): continue
        if dot != (kind == 'D'): continue
        if kind == 'L':
            attr = f'android:strokeColor="#FFFFFFFF" android:strokeWidth="{f(w)}" android:strokeLineCap="round" android:strokeLineJoin="round"'
        else:
            attr = 'android:fillColor="#FFFF6A1F"' if dot else 'android:fillColor="#FFFFFFFF"'
        path = f'<path android:pathData="{d}" {attr} />'
        if rot: path = f'<group android:rotation="{f(rot[0])}" android:pivotX="{f(rot[1])}" android:pivotY="{f(rot[2])}">{path}</group>'
        rows.append('    ' + path)
    return chr(10).join(['<?xml version="1.0" encoding="utf-8"?>',
        f'<!-- Wygenerowane z {name} skryptem generuj_kotlin.py. -->',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp"'
        ' android:viewportWidth="24" android:viewportHeight="24">'] + rows + ['</vector>', ''])


if mode == 'widzet':
    import os
    resdir, packs = src, sys.argv[4:]
    for old in os.listdir(resdir):
        if old.startswith('wi_'): os.remove(os.path.join(resdir, old))
    entries = []
    for pack in packs:
        sdir = pack + '/svg/jasne-tlo/'
        for fn in sorted(os.listdir(sdir)):
            stem = fn[:-4]
            if not fn.endswith('.svg') or stem.startswith(('ui-', 'tab-', 'akcja-', 'motyw-', 'powiad-')): continue
            res = 'wi_' + stem.replace('-', '_')
            parts = parts_of(sdir + fn)
            open(os.path.join(resdir, res + '_l.xml'), 'w', encoding='utf-8').write(vector_xml(parts, fn, False))
            open(os.path.join(resdir, res + '_d.xml'), 'w', encoding='utf-8').write(vector_xml(parts, fn, True))
            entries.append((stem, res))
    o = ['package pl.sportfamily.app.widget', '',
         '// Wygenerowane skryptem branding/ikony_zajecia/generuj_kotlin.py (tryb widzet). Nie edytuj ręcznie.', '',
         'import pl.sportfamily.app.R', '',
         '/** Ikony do widżetu: klucz ikony -> (linia do barwienia, pomarańczowa kropka). */',
         'val WidgetIcons: Map<String, Pair<Int, Int>> = mapOf(']
    o += [f'    "{k}" to (R.drawable.{r}_l to R.drawable.{r}_d),' for k, r in entries]
    o += [')', '']
    open(out, 'w', encoding='utf-8').write(chr(10).join(o))
    print(out, len(entries), 'ikon'); sys.exit(0)

if mode == 'drawable':
    # Mała ikona powiadomienia: wszystko białe (Android maluje ją jednym kolorem).
    rows = []
    for d, kind, w, rot in parts_of(src):
        if kind in ('C', 'CS'): continue
        attr = (f'android:strokeColor="#FFFFFFFF" android:strokeWidth="{f(w)}" android:strokeLineCap="round" android:strokeLineJoin="round"'
                if kind == 'L' else 'android:fillColor="#FFFFFFFF"')
        path = f'<path android:pathData="{d}" {attr} />'
        if rot: path = f'<group android:rotation="{f(rot[0])}" android:pivotX="{f(rot[1])}" android:pivotY="{f(rot[2])}">{path}</group>'
        rows.append('    ' + path)
    name = src.replace(chr(92), '/').split('/')[-1]
    xml = ['<?xml version="1.0" encoding="utf-8"?>',
           f'<!-- Wygenerowane z {name} skryptem generuj_kotlin.py. -->',
           '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp"'
           ' android:viewportWidth="24" android:viewportHeight="24">'] + rows + ['</vector>', '']
    open(out, 'w', encoding='utf-8').write('\n'.join(xml))
    print(out, 'gotowe'); sys.exit(0)

meta = json.load(open(src + '/ikony.json', encoding='utf-8'))
svg = src + '/svg/jasne-tlo/'
head = '''package pl.sportfamily.app.ui.components

// Wygenerowane skryptem branding/ikony_zajecia/generuj_kotlin.py z paczki SVG. Nie edytuj ręcznie.
'''

if mode == 'zajecia':
    # Poprawki słów kluczowych: chór ma własną ikonę, odmiana "pierwszej pomocy", dawne słowa tenisa i basenu.
    FIX = {'spiew.svg': {'remove': ['chór']}, 'pierwsza-pomoc.svg': {'add': ['pierwszej pom']},
           'tenis.svg': {'add': ['padel', 'squash']}, 'plywanie.svg': {'add': ['aqua']}}
    for x in meta:
        fx = FIX.get(x['plik'], {})
        x['slowa_kluczowe'] = [k for k in x['slowa_kluczowe'] if k not in fx.get('remove', [])] + fx.get('add', [])
    # Bardziej szczegółowe przed ogólniejszymi, sporty najpierw.
    BEFORE = [('tenis-stolowy.svg', 'tenis.svg'), ('pilka-reczna.svg', 'pilka-nozna.svg'), ('rugby.svg', 'pilka-nozna.svg'),
              ('pieczenie.svg', 'gotowanie.svg'), ('majsterkowanie.svg', 'elektronika.svg'), ('pierwsza-pomoc.svg', 'wolontariat.svg')]
    for a, b in BEFORE:
        names = [x['plik'] for x in meta]
        if a in names and b in names and names.index(a) > names.index(b):
            item = meta.pop(names.index(a))
            meta.insert([x['plik'] for x in meta].index(b), item)
    meta.sort(key=lambda x: 0 if x['grupa'].startswith('Sport') else 1)
    o = [head, '/** Ikony zajęć i sportów, w kolejności dopasowywania słów z tytułu. */',
         'val ActivityIcons: List<ActivityIcon> by lazy {', '    listOf(']
    for m in meta:
        keys = ', '.join('"%s"' % k for k in m['slowa_kluczowe'])
        o.append(f'        ActivityIcon("{m["plik"][:-4]}", "{m["nazwa"]}", "{m["grupa"]}", listOf({keys}), listOf(')
        o.append(kt_parts(parts_of(svg + m['plik']), '            '))
        o.append('        )),')
    o.append('    )\n}\n')
else:
    files = []
    for m in meta:
        files.append((m['plik'], m['nazwa'], m['grupa'], m.get('slowa_kluczowe', [])))
        if m.get('plik_wybrana'):
            files.append((m['plik_wybrana'], m['nazwa'] + ' (wybrana)', m['grupa'], []))
    o = [head, '/** Ikony aplikacji z paczki ' + src.replace(chr(92), '/').split('/')[-1] + '; klucz to nazwa pliku bez .svg. */',
         f'val {valname}: Map<String, ActivityIcon> by lazy {{', '    mapOf(']
    for plik, nazwa, grupa, keys in files:
        k = ', '.join('"%s"' % x for x in keys)
        o.append(f'        "{plik[:-4]}" to ActivityIcon("{plik[:-4]}", "{nazwa}", "{grupa}", listOf({k}), listOf(')
        o.append(kt_parts(parts_of(svg + plik), '            '))
        o.append('        )),')
    o.append('    )\n}\n')

open(out, 'w', encoding='utf-8').write('\n'.join(o))
print(out, 'gotowe')
