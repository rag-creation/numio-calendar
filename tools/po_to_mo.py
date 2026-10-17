# Minimal .po -> .mo compiler (same format as GNU msgfmt), for building the holiday tables.
import ast, struct, sys, pathlib
def parse(path):
    msgs={}; ctx=None; mid=None; mstr=None; sect=None; fuzzy=False
    def flush():
        if mid is not None and mstr is not None and not fuzzy and (mstr or mid==''):
            key=(ctx+'\x04' if ctx else '')+mid
            msgs[key]=mstr
    for line in open(path, encoding='utf-8'):
        line=line.strip()
        if line.startswith('#,') and 'fuzzy' in line:
            flush(); ctx=mid=mstr=None; fuzzy=True; continue
        if line.startswith('#') or not line:
            continue
        if line.startswith('msgctxt'):
            flush(); ctx=ast.literal_eval(line[7:].strip()); mid=mstr=None; sect='ctx'; fuzzy=False
        elif line.startswith('msgid '):
            if sect!='ctx': flush(); ctx=None; fuzzy=fuzzy if sect is None else False
            mid=ast.literal_eval(line[5:].strip()); mstr=None; sect='id'
        elif line.startswith('msgstr'):
            mstr=ast.literal_eval(line[6:].strip()); sect='str'
        elif line.startswith('"'):
            v=ast.literal_eval(line)
            if sect=='id': mid+=v
            elif sect=='str': mstr+=v
            elif sect=='ctx': ctx+=v
    flush()
    return msgs
def write(msgs, out):
    keys=sorted(msgs)
    ids=b''; strs=b''; offs=[]
    for k in keys:
        kb=k.encode(); vb=msgs[k].encode()
        offs.append((len(ids),len(kb),len(strs),len(vb)))
        ids+=kb+b'\0'; strs+=vb+b'\0'
    n=len(keys); keystart=7*4+16*n; valstart=keystart+len(ids)
    ko=[]; vo=[]
    for o1,l1,o2,l2 in offs:
        ko+=[l1,o1+keystart]; vo+=[l2,o2+valstart]
    data=struct.pack('Iiiiiii',0x950412de,0,n,7*4,7*4+n*8,0,0)
    data+=struct.pack(f'{len(ko)}i',*ko)+struct.pack(f'{len(vo)}i',*vo)+ids+strs
    open(out,'wb').write(data)
for po in pathlib.Path(sys.argv[1]).rglob('*.po'):
    write(parse(po), po.with_suffix('.mo'))
