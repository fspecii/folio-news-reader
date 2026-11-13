import sys, json, re, urllib.request, concurrent.futures as cf, time
sys.path.insert(0, __file__.rsplit('/',1)[0])
from candidates import C
UA = "Mozilla/5.0 (Linux; Android 14; SM-F946B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
def check(row):
    topic,name,url = row
    try:
        req = urllib.request.Request(url, headers={"User-Agent":UA,"Accept":"application/rss+xml,application/atom+xml,application/xml,text/xml,*/*;q=0.8"})
        with urllib.request.urlopen(req, timeout=14) as r:
            body = r.read(400_000).decode("utf-8","ignore")
        head = body[:4000].lower()
        if not ("<rss" in head or "<feed" in head or "<rdf:rdf" in head): return (row, False, "not a feed")
        items = len(re.findall(r"<item[ >]|<entry[ >]", body))
        if items < 3: return (row, False, f"only {items} items")
        return (row, True, items)
    except Exception as e:
        return (row, False, str(e)[:40])
seen=set(); rows=[]
for r in C:
    k=(r[0],r[2])
    if k in seen: continue
    seen.add(k); rows.append(r)
t=time.time()
with cf.ThreadPoolExecutor(24) as ex: res=list(ex.map(check, rows))
ok=[(r,i) for r,good,i in res if good]
bad=[(r,i) for r,good,i in res if not good]
print(f"{len(ok)} ok / {len(rows)} in {time.time()-t:.0f}s")
json.dump([{"topic":r[0],"name":r[1],"url":r[2],"items":i} for r,i in ok], open(__file__.rsplit('/',1)[0]+"/valid.json","w"), indent=1)
from collections import Counter
print(Counter(r[0] for r,_ in ok))
print("FAILED:", ", ".join(f"{r[1]}({i})" for r,i in bad)[:1800])
