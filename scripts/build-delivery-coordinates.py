"""Build an attributed offline GeoNames snapshot; never guess missing coordinates."""
from pathlib import Path
import re, unicodedata

root = Path(__file__).resolve().parent.parent
data = root / "bin/geodata"
def normal(s):
    s = ''.join(c for c in unicodedata.normalize('NFKD', s) if not unicodedata.combining(c)).lower()
    s = re.sub(r'\([^)]*\)', '', s)
    s = re.sub(r'\bsto\.', 'santo', s)
    s = re.sub(r'\bsta\.', 'santa', s)
    s = re.sub(r'^(province of |municipality of |city of )', '', s)
    s = re.sub(r' (province|city|municipality)$', '', s)
    return re.sub(r'[^a-z0-9]', '', s)
admin2 = {}
for line in (data / 'admin2Codes.txt').read_text(encoding='utf-8').splitlines():
    p = line.split('\t')
    if p[0].startswith('PH.'):
        admin2[p[0]] = p[1]
places = {}
for line in (data / 'PH.txt').read_text(encoding='utf-8').splitlines():
    p = line.split('\t')
    if len(p) < 19 or p[6] not in ('A', 'P'): continue
    province = admin2.get('PH.' + p[10] + '.' + p[11], '')
    score = 4 if p[7] == 'ADM3' else 3 if p[7].startswith('PPLA') else 2 if p[7] == 'ADM2' else 1
    if p[6] == 'A' and p[7] not in ('ADM2', 'ADM3'): continue
    for name in {p[1], p[2], *p[3].split(',')}:
        key = (normal(province), normal(name))
        candidate = (score, int(p[14] or 0), p[4], p[5], p[0])
        if candidate[:2] > places.get(key, (-1, -1))[:2]: places[key] = candidate
        if p[10] == 'NCR':
            key = ('metromanila', normal(name))
            if candidate[:2] > places.get(key, (-1, -1))[:2]: places[key] = candidate
directory = set()
for line in (root / 'src/data/delivery-addresses.tsv').read_text(encoding='utf-8').splitlines()[1:]:
    p = line.split('\t'); directory.add((p[0], p[1]))
output = ['province\tcity\tlatitude\tlongitude\tgeonames_id']
missing = []
for province, city in sorted(directory):
    candidate = places.get((normal(province), normal(city)))
    if candidate:
        output.append('\t'.join((province, city, candidate[2], candidate[3], candidate[4])))
    else: missing.append((province, city))
(root / 'src/data/delivery-coordinates.tsv').write_text('\n'.join(output)+'\n', encoding='utf-8')
print(f'Matched {len(output)-1}/{len(directory)} municipalities. Unknown destinations remain unranked, never guessed.')
print('Missing:', missing[:25])
