import json, re, collections, hashlib, urllib.parse, html, os
T = os.path.dirname(os.path.abspath(__file__))
valid = json.load(open(T + "/valid.json"))
pubs = json.load(open(T + "/publishers.json"))
fb = json.load(open(T + "/flipboard_topics.json"))["topics"]

CATS = [  # id, title, blurb
 ("news","World News","Top stories from around the globe"),("tech","Technology","Gadgets, software and the industry"),
 ("ai","Artificial Intelligence","Models, research and products"),("mobile","Phones & Gadgets","Android, Samsung, Apple and more"),
 ("dev","Programming","Code, tools and engineering blogs"),("security","Cybersecurity","Breaches, threats and privacy"),
 ("opensource","Linux & Makers","Open source, hardware and DIY electronics"),("science","Science","Discoveries and research"),
 ("space","Space","Missions, astronomy and rockets"),("business","Business & Finance","Markets, companies and money"),
 ("startups","Startups","Founders, funding and product launches"),("crypto","Crypto","Bitcoin, blockchain and markets"),
 ("politics","Politics","Government, elections and policy"),("sports","Sports","Scores, leagues and analysis"),
 ("gaming","Gaming","Games, consoles and esports"),("film","Film & TV","Movies, streaming and reviews"),
 ("music","Music","Albums, artists and concerts"),("celebrity","Celebrity","Entertainment news and pop culture"),
 ("design","Design & Art","Architecture, product and visual design"),("photo","Photography","Cameras, craft and inspiration"),
 ("style","Style","Fashion, beauty and streetwear"),("travel","Travel","Destinations, flights and guides"),
 ("food","Food","Recipes, restaurants and cooking"),("auto","Cars & EVs","Autos, motorsport and electric vehicles"),
 ("home","Home & Garden","Interiors, DIY and gardening"),("health","Health & Fitness","Wellbeing, medicine and training"),
 ("climate","Climate & Environment","Climate science and clean energy"),("culture","Culture & Ideas","Long reads, books, art and history"),
]
cat_ids = {c[0] for c in CATS}
SLUG = {  # Flipboard topic slug -> our category
 "news":"news","world-news":"news","us-news":"news","asia":"travel","europe":"travel",
 "technology":"tech","consumertechnology":"tech","enterprisetechnology":"tech","geek":"tech",
 "artificial-intelligence":"ai","ai":"ai","artificialintelligence":"ai","generativeai":"ai","anthropic":"ai",
 "apple":"mobile","apps":"mobile","cybersecurity":"security","programming":"dev",
 "science":"science","astronomy":"space","space":"space","biology":"science","brain":"science","archaeology":"science","anthropology":"science","aging":"health",
 "health":"health","fitness":"health","exercise":"health",
 "business":"business","finance":"business","investing":"business","capitalmarkets":"business","financialmarkets":"business","financialplanning":"business","chiefexecutiveofficer":"business",
 "startups":"startups","bitcoin":"crypto","cryptocurrency":"crypto","digitalcurrency":"crypto",
 "politics":"politics","donaldtrump":"politics",
 "sports":"sports","basketball":"sports","football":"sports","collegebasketball":"sports","collegesports":"sports","californiasports":"sports",
 "gaming":"gaming","film":"film","filmawardsseason":"film","drama":"film","television":"film",
 "music":"music","bands":"music","celebrity":"celebrity","celebritynews":"celebrity","celebritystyle":"celebrity","entertainment":"celebrity","entertainmentuk":"celebrity","awardshows":"celebrity",
 "design":"design","architecture":"design","art":"design","artexhibition":"design","artgalleries":"design","arthistory":"design","arts":"design",
 "photography":"photo","camera":"photo","digitalphotography":"photo",
 "style":"style","fashion":"style","fashiontrends":"style","clothes":"style","beauty":"style",
 "travel":"travel","flights":"travel","food":"food","cooking":"food","foodcooking":"food",
 "auto":"auto","automotive":"auto","home":"home",
 "climate":"climate","climate-change":"climate","climatechange":"climate","environment":"climate",
 "books":"culture","bookpublishing":"culture","fiction":"culture","culture":"culture","history":"culture","education":"culture","college":"culture",
}
DROP = {"alicehuang","colettekress","browardcounty","crime"}
for s in fb:
    if s in DROP: continue
    if s not in SLUG:   # inherit from the main topic that links to it most
        votes = collections.Counter()
        for p, v in fb.items():
            if p in SLUG:
                for r, c in v["related"]:
                    if r == s: votes[SLUG[p]] += c
        if votes: SLUG[s] = votes.most_common(1)[0][0]

def sid(url): return "c" + hashlib.sha1(url.encode()).hexdigest()[:10]
def clean(name):
    n = html.unescape(name or "").strip()
    n = re.sub(r"\s*[\|\-–—:·»]\s*(RSS|Feed|Home|Latest|Top Stories|Official Site|Homepage|News).*$", "", n, flags=re.I)
    n = re.sub(r"\s+(RSS|Feed)$", "", n, flags=re.I).strip()
    return n[:38] if len(n) > 1 else None
def host(u):
    h = urllib.parse.urlparse(u).netloc.lower()
    return re.sub(r"^(www|feeds|rss|api)\.", "", h)

sources, seen_url, seen_cat_host = [], set(), set()
per_cat = collections.Counter()
for v in valid:                       # 1. hand-picked, verified
    if v["topic"] not in cat_ids or v["url"] in seen_url: continue
    seen_url.add(v["url"]); seen_cat_host.add((v["topic"], host(v["url"])))
    feat = per_cat[v["topic"]] < 3; per_cat[v["topic"]] += 1
    sources.append({"id": sid(v["url"]), "name": v["name"], "url": v["url"], "topic": v["topic"], "kind": "publisher", "featured": feat})

# 2. Flipboard's real publishers, placed by where Flipboard lists them
dom_votes = collections.defaultdict(collections.Counter)
for s, v in fb.items():
    c = SLUG.get(s)
    if not c: continue
    for d, n in v["publishers"]: dom_votes[d][c] += n
skip_dom = {"flipboard.com", "aol.com", "yahoo.com", "msn.com"}
for d, info in sorted(pubs.items()):
    if d in skip_dom or d not in dom_votes: continue
    cat = dom_votes[d].most_common(1)[0][0]
    h = host(info["feed"])
    if info["feed"] in seen_url or any(host(u) == h for u in [x["url"] for x in sources if x["topic"] == cat]): continue
    name = clean(info["title"]) or d
    seen_url.add(info["feed"])
    sources.append({"id": sid(info["feed"]), "name": name, "url": info["feed"], "topic": cat, "kind": "publisher", "featured": False, "via": "flipboard"})

# 3. Flipboard's own topic feeds (ranked by Flipboard)
main_feed = {"news":"news","tech":"technology","ai":"artificial-intelligence","design":"design","photo":"photography","business":"business","sports":"sports",
             "style":"style","travel":"travel","politics":"politics","food":"food","music":"music","film":"film","gaming":"gaming","auto":"auto","science":"science",
             "home":"home","celebrity":"celebrity","health":"health","space":"space","security":"cybersecurity","startups":"startups","crypto":"cryptocurrency","climate":"climate-change"}
for s, v in fb.items():
    c = SLUG.get(s)
    if not c or s in ("ai","artificialintelligence","climate","climatechange","celebritynews"): continue
    title = v["rss"]["title"].strip()
    sources.append({"id": sid("fb:" + s), "name": "Flipboard · " + title, "url": f"https://flipboard.com/topic/{s}.rss", "topic": c,
                    "kind": "flipboard", "featured": main_feed.get(c) == s})

out = {"topics": [{"id": i, "title": t, "blurb": b} for i, t, b in CATS], "sources": sources}
dest = T + "/../app/src/main/assets"; os.makedirs(dest, exist_ok=True)
json.dump(out, open(dest + "/catalog.json", "w"), ensure_ascii=False, separators=(",", ":"))
bc = collections.Counter((s["topic"], s["kind"]) for s in sources)
print("sources:", len(sources), "| publishers:", sum(1 for s in sources if s["kind"]=="publisher"), "| flipboard topics:", sum(1 for s in sources if s["kind"]=="flipboard"),
      "| via flipboard publishers:", sum(1 for s in sources if s.get("via")))
print({c: sum(n for (t, k), n in bc.items() if t == c) for c, _, _ in CATS})
print(os.path.getsize(dest + "/catalog.json"), "bytes")
