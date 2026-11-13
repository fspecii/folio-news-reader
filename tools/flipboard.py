"""Collect Flipboard's real topics + publishers from its public web pages / RSS (read-only, polite)."""
import re, json, time, urllib.request, collections, concurrent.futures as cf
UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
def get(url, limit=3_500_000):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=25) as r:
        return r.read(limit).decode("utf-8", "ignore")

SEED = ["news","technology","design","photography","business","sports","style","travel","politics","food","music",
        "film","gaming","auto","science","home","celebrity","artificial-intelligence","ai","health","space","climate-change",
        "cybersecurity","programming","startups","cryptocurrency","books","art","fitness","education","history","television",
        "environment","fashion","architecture","world-news","us-news","finance","investing","football","basketball"]
SKIP = {"topic","topics","explore","trending","search","new","popular"}

def page(slug):
    try:
        h = get(f"https://flipboard.com/topic/{slug}")
    except Exception as e:
        return slug, None
    rel = collections.Counter(m for m in re.findall(r'/topic/([a-z0-9][a-z0-9-]{1,40})(?=["?/#\\])', h) if m not in SKIP and m != slug)
    pubs = collections.Counter(d for d in re.findall(r'"sourceDomain"\s*:\s*"([^"]+)"', h) if d not in ("flipboard.com",))
    handles = collections.Counter(re.findall(r'"sourceURL"\s*:\s*"https://flipboard.com/(@[A-Za-z0-9_.-]+)"', h))
    return slug, {"related": rel.most_common(25), "publishers": pubs.most_common(40), "handles": handles.most_common(40)}

def rss(slug):
    try:
        x = get(f"https://flipboard.com/topic/{slug}.rss", 800_000)
    except Exception:
        return slug, None
    n = len(re.findall(r"<item>", x))
    t = re.search(r"<channel>\s*<title>([^<]+)</title>", x)
    return slug, {"items": n, "title": t.group(1).strip() if t else slug}

data = {}
t0 = time.time()
with cf.ThreadPoolExecutor(4) as ex:
    for slug, d in ex.map(page, SEED):
        if d: data[slug] = d
print("seed pages ok:", len(data), "/", len(SEED), f"{time.time()-t0:.0f}s")

# one level of related topics, most-linked first
cand = collections.Counter()
for d in data.values():
    for s, c in d["related"]: cand[s] += c
extra = [s for s, _ in cand.most_common(140) if s not in data]
with cf.ThreadPoolExecutor(4) as ex:
    for slug, d in ex.map(page, extra):
        if d: data[slug] = d
print("topics with pages:", len(data), f"{time.time()-t0:.0f}s")

valid = {}
with cf.ThreadPoolExecutor(4) as ex:
    for slug, d in ex.map(rss, list(data)):
        if d and d["items"] >= 10: valid[slug] = d
print("topics with a live RSS feed (>=10 items):", len(valid), f"{time.time()-t0:.0f}s")
for s in valid: data[s]["rss"] = valid[s]
json.dump({"topics": {s: data[s] for s in valid}}, open(__file__.rsplit("/",1)[0] + "/flipboard_topics.json", "w"), indent=1)
print(sorted(valid)[:80])
