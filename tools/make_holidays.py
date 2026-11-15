# Makes the per-country tables (zipped into app/src/main/assets/holidays.zip) from a checkout of https://github.com/vacanza/holidays (MIT).
# Run: python3 tools/po_to_mo.py <holidays>/holidays/locale && python3 tools/make_holidays.py <holidays> <out>
# where <holidays> is the holidays checkout and <out> an empty folder. Tables were made from holidays commit d3261e8.
# Then zip the tables: cd <out> && zip -X -9 holidays.zip *.txt (and copy it to app/src/main/assets/).
import sys, os, warnings
if len(sys.argv) != 3:
    sys.exit('usage: make_holidays.py <holidays checkout> <output folder>')
sys.path.insert(0, sys.argv[1])
warnings.filterwarnings('ignore')
import holidays
out=sys.argv[2]
years=range(2000, 2051)
countries=holidays.list_supported_countries()
done=[]
def write(code, cls_kwargs, label=None):
    try:
        h=holidays.country_holidays(**cls_kwargs, years=years)
        cls=type(h)
        langs=getattr(cls,'supported_languages',()) or ()
        lang=next((l for l in ('en_US','en_GB','en') if l in langs), None)
        if lang: h=holidays.country_holidays(**cls_kwargs, years=years, language=lang)
        rows=sorted((d.isoformat(), n) for d,n in h.items())
        if not rows: return
        names=[]; idx={}
        lines=[]
        for d,n in rows:
            for part in n.split('; '):
                if part not in idx: idx[part]=len(names); names.append(part)
                lines.append(d.replace('-','')+'\t'+str(idx[part]))
        with open(f'{out}/{code}.txt','w') as f:
            f.write('\n'.join(names)+'\n--\n'+'\n'.join(lines)+'\n')
        done.append((code,len(rows)))
    except Exception as e:
        print('skip',code,e,file=sys.stderr)
for c in sorted(countries):
    write(c, dict(country=c))
write('IN-KL', dict(country='IN', subdiv='KL'))
print(len(done), sum(n for _,n in done))
