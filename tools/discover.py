"""For every publisher domain Flipboard lists, find a working RSS/Atom feed (autodiscovery + common paths)."""
import re, json, urllib.request, urllib.parse, concurrent.futures as cf, time, html
UA = "Mozilla/5.0 (Linux; Android 14; SM-F946B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
def get(url, limit=500_000, timeout=10):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "text/html,application/rss+xml,application/atom+xml,application/xml,*/*;q=0.8"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read(limit).decode("utf-8", "ignore"), r.geturl()
def is_feed(b):
    h = b[:4000].lower()
    return ("<rss" in h or "<feed" in h or "<rdf:rdf" in h) and len(re.findall(r"<item[ >]|<entry[ >]", b)) >= 3
def title(b):
    m = re.search(r"<title[^>]*>\s*(?:<!\[CDATA\[)?(.*?)(?:\]\]>)?\s*</title>", b, re.S)
    return html.unescape(m.group(1)).strip() if m else None
def discover(domain):
    base = "https://" + domain
    cands = []
    try:
        body, final = get(base + "/")
        for m in re.finditer(r"<link[^>]+>", body, re.I):
            tag = m.group(0)
            if re.search(r'type=["\']application/(rss|atom)\+xml', tag, re.I) and re.search(r'rel=["\']alternate', tag, re.I):
                h = re.search(r'href=["\']([^"\']+)', tag)
                if h: cands.append(urllib.parse.urljoin(final, html.unescape(h.group(1))))
    except Exception:
        pass
    for p in ("/feed", "/rss", "/feed/", "/rss.xml", "/feed.xml", "/index.xml", "/atom.xml", "/rss/"):
        cands.append(base + p)
    seen = set()
    for c in cands:
        if c in seen: continue
        seen.add(c)
        if len(seen) > 7: break
        try:
            b, _ = get(c)
            if is_feed(b): return domain, c, title(b)
        except Exception:
            continue
    return domain, None, None

t = json.load(open(__file__.rsplit("/", 1)[0] + "/flipboard_topics.json"))["topics"]
domains = sorted({d for v in t.values() for d, _ in v["publishers"]})
t0 = time.time()
with cf.ThreadPoolExecutor(14) as ex: res = list(ex.map(discover, domains))
ok = {d: {"feed": f, "title": ti} for d, f, ti in res if f}
print(f"{len(ok)} of {len(domains)} publishers have a working feed ({time.time()-t0:.0f}s)")
json.dump(ok, open(__file__.rsplit("/", 1)[0] + "/publishers.json", "w"), indent=1)
print(", ".join(list(ok)[:60]))
