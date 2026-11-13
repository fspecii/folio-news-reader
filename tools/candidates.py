# (topic, name, url) — candidates; tools/validate.py keeps only feeds that actually respond.
C = []
def add(topic, rows):
    for n,u in rows: C.append((topic,n,u))

add("news", [
 ("BBC World","https://feeds.bbci.co.uk/news/world/rss.xml"),("NPR News","https://feeds.npr.org/1001/rss.xml"),
 ("Al Jazeera","https://www.aljazeera.com/xml/rss/all.xml"),("The Guardian World","https://www.theguardian.com/world/rss"),
 ("DW News","https://rss.dw.com/rdf/rss-en-all"),("France 24","https://www.france24.com/en/rss"),
 ("NYT World","https://rss.nytimes.com/services/xml/rss/nyt/World.xml"),("NYT Home","https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml"),
 ("Washington Post World","https://feeds.washingtonpost.com/rss/world"),("CNN Top Stories","http://rss.cnn.com/rss/edition.rss"),
 ("Sky News World","https://feeds.skynews.com/feeds/rss/world.xml"),("ABC News","https://abcnews.go.com/abcnews/internationalheadlines"),
 ("CBS News","https://www.cbsnews.com/latest/rss/world"),("The Independent","https://www.independent.co.uk/news/world/rss"),
 ("Euronews","https://www.euronews.com/rss"),("Japan Times","https://www.japantimes.co.jp/feed/"),
 ("ABC News Australia","https://www.abc.net.au/news/feed/51120/rss.xml"),("CBC World","https://www.cbc.ca/webfeed/rss/rss-world"),
 ("The Atlantic","https://www.theatlantic.com/feed/all/"),("Vox","https://www.vox.com/rss/index.xml"),("Axios","https://api.axios.com/feed/"),
 ("TIME","https://time.com/feed/"),("The Economist","https://www.economist.com/the-world-this-week/rss.xml"),("Foreign Policy","https://foreignpolicy.com/feed/"),
 ("The Hindu","https://www.thehindu.com/news/international/feeder/default.rss"),("Reuters via Google","https://news.google.com/rss"),
])
add("tech", [
 ("The Verge","https://www.theverge.com/rss/index.xml"),("Ars Technica","https://feeds.arstechnica.com/arstechnica/index"),
 ("TechCrunch","https://techcrunch.com/feed/"),("WIRED","https://www.wired.com/feed/rss"),("Engadget","https://www.engadget.com/rss.xml"),
 ("Hacker News","https://hnrss.org/frontpage"),("The Register","https://www.theregister.com/headlines.atom"),("404 Media","https://www.404media.co/rss/"),
 ("CNET","https://www.cnet.com/rss/news/"),("ZDNET","https://www.zdnet.com/news/rss.xml"),("Gizmodo","https://gizmodo.com/feed"),
 ("Mashable","https://mashable.com/feeds/rss/all"),("TechRadar","https://www.techradar.com/rss"),("Tom's Hardware","https://www.tomshardware.com/feeds/all"),
 ("Slashdot","http://rss.slashdot.org/Slashdot/slashdotMain"),("The Next Web","https://thenextweb.com/feed"),("Techmeme","https://www.techmeme.com/feed.xml"),
 ("Daring Fireball","https://daringfireball.net/feeds/main"),("Lifehacker","https://lifehacker.com/feed/rss"),("PCMag","https://www.pcmag.com/feeds/rss/latest"),
 ("Digital Trends","https://www.digitaltrends.com/feed/"),("Wccftech","https://wccftech.com/feed/"),("Rest of World","https://restofworld.org/feed/"),
 ("MIT Technology Review","https://www.technologyreview.com/feed/"),("IEEE Spectrum","https://spectrum.ieee.org/feeds/feed.rss"),("Stratechery","https://stratechery.com/feed/"),
])
add("ai", [
 ("MIT Technology Review · AI","https://www.technologyreview.com/topic/artificial-intelligence/feed"),("TechCrunch · AI","https://techcrunch.com/category/artificial-intelligence/feed/"),
 ("The Decoder","https://the-decoder.com/feed/"),("OpenAI News","https://openai.com/news/rss.xml"),("The Verge · AI","https://www.theverge.com/rss/ai-artificial-intelligence/index.xml"),
 ("Ars Technica · AI","https://arstechnica.com/ai/feed/"),("Hugging Face Blog","https://huggingface.co/blog/feed.xml"),("Google DeepMind","https://deepmind.google/blog/rss.xml"),
 ("Import AI","https://importai.substack.com/feed"),("Google AI Blog","https://blog.google/technology/ai/rss/"),("Microsoft AI","https://blogs.microsoft.com/ai/feed/"),
 ("NVIDIA Blog","https://blogs.nvidia.com/feed/"),("AWS Machine Learning","https://aws.amazon.com/blogs/machine-learning/feed/"),("BAIR Blog","https://bair.berkeley.edu/blog/feed.xml"),
 ("MarkTechPost","https://www.marktechpost.com/feed/"),("AI News","https://www.artificialintelligence-news.com/feed/"),("Last Week in AI","https://lastweekin.ai/feed"),
 ("Simon Willison","https://simonwillison.net/atom/everything/"),("Ahead of AI","https://magazine.sebastianraschka.com/feed"),("Latent Space","https://www.latent.space/feed"),
 ("Interconnects","https://www.interconnects.ai/feed"),("WIRED · AI","https://www.wired.com/feed/tag/ai/latest/rss"),("Synced","https://syncedreview.com/feed/"),
 ("Anthropic (unofficial)","https://raw.githubusercontent.com/Olshansk/rss-feeds/main/feeds/feed_anthropic_news.xml"),("Towards Data Science","https://towardsdatascience.com/feed"),
])
add("mobile", [
 ("SamMobile","https://www.sammobile.com/feed/"),("Android Authority","https://www.androidauthority.com/feed/"),("Android Police","https://www.androidpolice.com/feed/"),
 ("9to5Google","https://9to5google.com/feed/"),("GSMArena","https://www.gsmarena.com/rss-news-reviews.php3"),("XDA","https://www.xda-developers.com/feed/"),
 ("9to5Mac","https://9to5mac.com/feed/"),("MacRumors","https://feeds.macrumors.com/MacRumors-All"),("Droid Life","https://www.droid-life.com/feed/"),
 ("PhoneArena","https://www.phonearena.com/feed"),("Android Central","https://www.androidcentral.com/feed"),("iMore","https://www.imore.com/feed"),
 ("AppleInsider","https://appleinsider.com/rss/news/"),("9to5Toys","https://9to5toys.com/feed/"),("Notebookcheck","https://www.notebookcheck.net/News.152.100.html?type=rss"),
])
add("dev", [
 ("Hacker News: Best","https://hnrss.org/best"),("Lobsters","https://lobste.rs/rss"),("GitHub Blog","https://github.blog/feed/"),("Stack Overflow Blog","https://stackoverflow.blog/feed/"),
 ("DEV Community","https://dev.to/feed"),("CSS-Tricks","https://css-tricks.com/feed/"),("Smashing Magazine","https://www.smashingmagazine.com/feed/"),
 ("JavaScript Weekly","https://javascriptweekly.com/rss/"),("Real Python","https://realpython.com/atom.xml"),("Rust Blog","https://blog.rust-lang.org/feed.xml"),
 ("Go Blog","https://go.dev/blog/feed.atom"),("Android Developers","https://feeds.feedburner.com/blogspot/hsDu"),("Kotlin Blog","https://blog.jetbrains.com/kotlin/feed/"),
 ("Cloudflare Blog","https://blog.cloudflare.com/rss/"),("Netflix Tech Blog","https://netflixtechblog.com/feed"),("Julia Evans","https://jvns.ca/atom.xml"),
 ("Martin Fowler","https://martinfowler.com/feed.atom"),("InfoQ","https://feed.infoq.com/"),("The New Stack","https://thenewstack.io/feed/"),("Coding Horror","https://blog.codinghorror.com/rss/"),
 ("Joel on Software","https://www.joelonsoftware.com/feed/"),("Overreacted","https://overreacted.io/rss.xml"),("Python Insider","https://blog.python.org/feeds/posts/default"),
])
add("security", [
 ("Krebs on Security","https://krebsonsecurity.com/feed/"),("BleepingComputer","https://www.bleepingcomputer.com/feed/"),("The Hacker News","https://feeds.feedburner.com/TheHackersNews"),
 ("Schneier on Security","https://www.schneier.com/feed/atom/"),("Dark Reading","https://www.darkreading.com/rss.xml"),("Ars Technica · Security","https://arstechnica.com/security/feed/"),
 ("Troy Hunt","https://www.troyhunt.com/rss/"),("Malwarebytes Labs","https://www.malwarebytes.com/blog/feed/index.xml"),("SecurityWeek","https://feeds.feedburner.com/securityweek"),
 ("Sophos News","https://nakedsecurity.sophos.com/feed/"),("WIRED · Security","https://www.wired.com/feed/category/security/latest/rss"),("Project Zero","https://googleprojectzero.blogspot.com/feeds/posts/default"),
 ("The Record","https://therecord.media/feed"),("CISA Alerts","https://www.cisa.gov/cybersecurity-advisories/all.xml"),("Graham Cluley","https://grahamcluley.com/feed/"),
])
add("opensource", [
 ("Phoronix","https://www.phoronix.com/rss.php"),("It's FOSS","https://itsfoss.com/feed/"),("LWN.net","https://lwn.net/headlines/rss"),("9to5Linux","https://9to5linux.com/feed"),
 ("OMG! Ubuntu","https://www.omgubuntu.co.uk/feed"),("Fedora Magazine","https://fedoramagazine.org/feed/"),("Hackaday","https://hackaday.com/blog/feed/"),("OSnews","https://www.osnews.com/feed/"),
 ("Adafruit","https://blog.adafruit.com/feed/"),("Arduino Blog","https://blog.arduino.cc/feed/"),("Raspberry Pi","https://www.raspberrypi.com/news/feed/"),("MAKE","https://makezine.com/feed/"),
 ("Linuxiac","https://linuxiac.com/feed/"),("GNOME News","https://news.gnome.org/feed/"),("KDE News","https://kde.org/index.xml"),("Hackster.io","https://www.hackster.io/news.atom"),
])
add("science", [
 ("NASA Breaking News","https://www.nasa.gov/feed/"),("Nature News","https://www.nature.com/nature.rss"),("ScienceDaily","https://www.sciencedaily.com/rss/all.xml"),
 ("Phys.org","https://phys.org/rss-feed/"),("New Scientist","https://www.newscientist.com/feed/home/"),("Quanta Magazine","https://api.quantamagazine.org/feed/"),
 ("Ars Technica · Science","https://arstechnica.com/science/feed/"),("Scientific American","http://rss.sciam.com/ScientificAmerican-Global"),("Live Science","https://www.livescience.com/feeds/all"),
 ("Science News","https://www.sciencenews.org/feed"),("Popular Science","https://www.popsci.com/feed/"),("Smithsonian","https://www.smithsonianmag.com/rss/latest_articles/"),
 ("Aeon","https://aeon.co/feed.rss"),("Big Think","https://bigthink.com/feed/"),("Science (AAAS)","https://www.science.org/rss/news_current.xml"),
 ("MIT News","https://news.mit.edu/rss/feed"),("Nautilus","https://nautil.us/feed/"),("Knowable Magazine","https://knowablemagazine.org/rss"),
])
add("space", [
 ("NASA Breaking News","https://www.nasa.gov/feed/"),("Space.com","https://www.space.com/feeds/all"),("SpaceNews","https://spacenews.com/feed/"),("Universe Today","https://www.universetoday.com/feed"),
 ("ESA","https://www.esa.int/rssfeed/Our_Activities/Space_News"),("Ars Technica · Space","https://arstechnica.com/space/feed/"),("Planetary Society","https://www.planetary.org/rss/articles"),
 ("NASA Image of the Day","https://www.nasa.gov/feeds/iotd-feed/"),("Sky & Telescope","https://skyandtelescope.org/feed/"),("EarthSky","https://earthsky.org/feed/"),
 ("Spaceflight Now","https://spaceflightnow.com/feed/"),("NASASpaceflight","https://www.nasaspaceflight.com/feed/"),("SpaceX Updates","https://www.space.com/feeds/tag/spacex"),
])
add("business", [
 ("Bloomberg Markets","https://feeds.bloomberg.com/markets/news.rss"),("CNBC","https://www.cnbc.com/id/100003114/device/rss/rss.html"),("WSJ World","https://feeds.a.wsj.com/rss/RSSWorldNews.xml"),
 ("WSJ Markets","https://feeds.a.wsj.com/rss/RSSMarketsMain.xml"),("Forbes","https://www.forbes.com/innovation/feed2"),("Fortune","https://fortune.com/feed/"),
 ("Business Insider","https://feeds.businessinsider.com/custom/all"),("MarketWatch","http://feeds.marketwatch.com/marketwatch/topstories/"),("Yahoo Finance","https://finance.yahoo.com/news/rssindex"),
 ("Harvard Business Review","https://feeds.hbr.org/harvardbusiness"),("Inc.","https://www.inc.com/rss"),("Entrepreneur","https://www.entrepreneur.com/latest.rss"),
 ("Fast Company","https://www.fastcompany.com/latest/rss"),("Seeking Alpha","https://seekingalpha.com/market_currents.xml"),("The Motley Fool","https://www.fool.com/a/feeds/foolwatch?id=foolwatch&format=rss2"),
 ("Financial Times","https://www.ft.com/rss/home"),("Barron's","https://www.barrons.com/xml/rss/3_7510"),("Morning Brew","https://www.morningbrew.com/daily/feed"),
])
add("startups", [
 ("Product Hunt","https://www.producthunt.com/feed"),("Crunchbase News","https://news.crunchbase.com/feed/"),("Sifted","https://sifted.eu/feed"),("First Round Review","https://review.firstround.com/feed.xml"),
 ("a16z","https://a16z.com/feed/"),("Paul Graham","http://www.aaronsw.com/2002/feeds/pgessays.rss"),("Y Combinator","https://www.ycombinator.com/blog/rss/"),("Indie Hackers","https://www.indiehackers.com/feed.xml"),
 ("TechCrunch · Startups","https://techcrunch.com/category/startups/feed/"),("SaaStr","https://www.saastr.com/feed/"),("Both Sides of the Table","https://bothsidesofthetable.com/feed"),
 ("Hacker News: Show HN","https://hnrss.org/show"),("Failory","https://www.failory.com/feed"),("VentureBeat","https://venturebeat.com/feed/"),
])
add("crypto", [
 ("CoinDesk","https://www.coindesk.com/arc/outboundfeeds/rss/"),("Cointelegraph","https://cointelegraph.com/rss"),("Decrypt","https://decrypt.co/feed"),("The Block","https://www.theblock.co/rss.xml"),
 ("Bitcoin Magazine","https://bitcoinmagazine.com/.rss/full/"),("Bankless","https://www.bankless.com/rss/feed"),("CryptoSlate","https://cryptoslate.com/feed/"),("BeInCrypto","https://beincrypto.com/feed/"),
])
add("politics", [
 ("POLITICO","https://www.politico.com/rss/politicopicks.xml"),("The Hill","https://thehill.com/feed/"),("NPR Politics","https://feeds.npr.org/1014/rss.xml"),
 ("Guardian US Politics","https://www.theguardian.com/us-news/us-politics/rss"),("NYT Politics","https://rss.nytimes.com/services/xml/rss/nyt/Politics.xml"),
 ("BBC Politics","https://feeds.bbci.co.uk/news/politics/rss.xml"),("Axios Politics","https://api.axios.com/feed/"),("FiveThirtyEight","https://fivethirtyeight.com/features/feed/"),
 ("RealClearPolitics","https://www.realclearpolitics.com/index.xml"),("Roll Call","https://rollcall.com/feed/"),("Washington Post Politics","https://feeds.washingtonpost.com/rss/politics"),
 ("The Intercept","https://theintercept.com/feed/?rss"),("Reason","https://reason.com/feed/"),("Mother Jones","https://www.motherjones.com/feed/"),("National Review","https://www.nationalreview.com/feed/"),
])
add("sports", [
 ("ESPN","https://www.espn.com/espn/rss/news"),("BBC Sport","https://feeds.bbci.co.uk/sport/rss.xml"),("Guardian Sport","https://www.theguardian.com/sport/rss"),("Sky Sports","https://www.skysports.com/rss/12040"),
 ("CBS Sports","https://www.cbssports.com/rss/headlines/"),("Yahoo Sports","https://sports.yahoo.com/rss/"),("BBC Football","https://feeds.bbci.co.uk/sport/football/rss.xml"),
 ("Formula 1","https://www.formula1.com/content/fom-website/en/latest/all.xml"),("Motorsport.com","https://www.motorsport.com/rss/all/news/"),("MMA Fighting","https://www.mmafighting.com/rss/current"),
 ("Sports Illustrated","https://www.si.com/rss/si_topstories.rss"),("The Athletic","https://www.nytimes.com/athletic/rss/news/"),("Bleacher Report","https://bleacherreport.com/articles/feed"),
 ("Goal.com","https://www.goal.com/feeds/en/news"),("Cycling News","https://www.cyclingnews.com/feeds/all"),("Tennis.com","https://www.tennis.com/rss/news"),
])
add("gaming", [
 ("IGN","https://feeds.ign.com/ign/all"),("Kotaku","https://kotaku.com/rss"),("Polygon","https://www.polygon.com/rss/index.xml"),("PC Gamer","https://www.pcgamer.com/rss/"),
 ("Eurogamer","https://www.eurogamer.net/feed"),("GameSpot","https://www.gamespot.com/feeds/mashup/"),("Rock Paper Shotgun","https://www.rockpapershotgun.com/feed"),("VG247","https://www.vg247.com/feed"),
 ("GamesRadar+","https://www.gamesradar.com/rss/"),("Destructoid","https://www.destructoid.com/feed/"),("Nintendo Life","https://www.nintendolife.com/feeds/latest"),("Push Square","https://www.pushsquare.com/feeds/latest"),
 ("GamesIndustry.biz","https://www.gamesindustry.biz/feed"),("TouchArcade","https://toucharcade.com/feed/"),("Giant Bomb","https://www.giantbomb.com/feeds/news/"),("Game Developer","https://www.gamedeveloper.com/rss.xml"),
 ("Steam News","https://store.steampowered.com/feeds/news/"),("Windows Central Gaming","https://www.windowscentral.com/gaming/rss"),
])
add("film", [
 ("Variety","https://variety.com/feed/"),("The Hollywood Reporter","https://www.hollywoodreporter.com/feed/"),("Deadline","https://deadline.com/feed/"),("IndieWire","https://www.indiewire.com/feed/"),
 ("/Film","https://www.slashfilm.com/feed/"),("Screen Rant","https://screenrant.com/feed/"),("Collider","https://collider.com/feed/"),("The A.V. Club","https://www.avclub.com/rss"),
 ("RogerEbert.com","https://www.rogerebert.com/feed"),("Vulture","https://www.vulture.com/rss/index.xml"),("Den of Geek","https://www.denofgeek.com/feed/"),("IGN Movies","https://feeds.ign.com/ign/movies-all"),
 ("CinemaBlend","https://www.cinemablend.com/rss/topic/news/movies"),("Empire","https://www.empireonline.com/feed/"),("Film Comment","https://www.filmcomment.com/feed/"),("TheWrap","https://www.thewrap.com/feed/"),
])
add("music", [
 ("Pitchfork","https://pitchfork.com/feed/feed-news/rss"),("Rolling Stone","https://www.rollingstone.com/feed/"),("Billboard","https://www.billboard.com/feed/"),("NME","https://www.nme.com/feed"),
 ("Consequence","https://consequence.net/feed/"),("Stereogum","https://www.stereogum.com/feed/"),("SPIN","https://www.spin.com/feed/"),("Loudwire","https://loudwire.com/feed/"),
 ("Resident Advisor","https://ra.co/xml/news.xml"),("The Quietus","https://thequietus.com/feed"),("Music Radar","https://www.musicradar.com/rss"),("Hypebeast Music","https://hypebeast.com/music/feed"),
 ("Bandcamp Daily","https://daily.bandcamp.com/feed"),("Metal Injection","https://metalinjection.net/feed"),("Complex Music","https://www.complex.com/music/rss"),
])
add("design", [
 ("Dezeen","https://www.dezeen.com/feed/"),("designboom","https://www.designboom.com/feed/"),("Creative Bloq","https://www.creativebloq.com/feed"),("It's Nice That","https://www.itsnicethat.com/rss"),
 ("Core77","https://feeds.feedburner.com/core77/blog"),("A List Apart","https://alistapart.com/main/feed/"),("Colossal","https://www.thisiscolossal.com/feed/"),("Booooooom","https://www.booooooom.com/feed/"),
 ("ArchDaily","https://www.archdaily.com/feed"),("Yanko Design","https://www.yankodesign.com/feed/"),("Nielsen Norman Group","https://www.nngroup.com/feed/rss/"),("Architectural Digest","https://www.architecturaldigest.com/feed/rss"),
 ("Fast Company Design","https://www.fastcompany.com/co-design/rss"),("Design Milk","https://design-milk.com/feed/"),("Eye on Design","https://eyeondesign.aiga.org/feed/"),("UX Collective","https://uxdesign.cc/feed"),
])
add("photo", [
 ("PetaPixel","https://petapixel.com/feed/"),("DPReview","https://www.dpreview.com/feeds/news.xml"),("Fstoppers","https://fstoppers.com/rss.xml"),("Digital Photography School","https://digital-photography-school.com/feed/"),
 ("500px ISO","https://iso.500px.com/feed/"),("Photofocus","https://photofocus.com/feed/"),("Fuji Rumors","https://www.fujirumors.com/feed/"),("Sony Alpha Rumors","https://www.sonyalpharumors.com/feed/"),
 ("The Phoblographer","https://www.thephoblographer.com/feed/"),("Digital Camera World","https://www.digitalcameraworld.com/feeds/all"),("PhotoRumors","https://photorumors.com/feed/"),
 ("Lens Culture","https://www.lensculture.com/rss"),("Time LightBox","https://time.com/section/photography/feed/"),("Canon Rumors","https://www.canonrumors.com/feed/"),("Nikon Rumors","https://nikonrumors.com/feed/"),
])
add("style", [
 ("Vogue","https://www.vogue.com/feed/rss"),("GQ","https://www.gq.com/feed/rss"),("Highsnobiety","https://www.highsnobiety.com/feed/"),("Hypebeast","https://hypebeast.com/feed"),
 ("Fashionista","https://fashionista.com/.rss/excerpt/"),("Esquire","https://www.esquire.com/rss/all.xml/"),("Who What Wear","https://www.whowhatwear.com/rss"),("Business of Fashion","https://www.businessoffashion.com/arc/outboundfeeds/rss/"),
 ("ELLE","https://www.elle.com/rss/all.xml/"),("Harper's Bazaar","https://www.harpersbazaar.com/rss/all.xml/"),("Sneaker News","https://sneakernews.com/feed/"),("Refinery29","https://www.refinery29.com/en-us/rss.xml"),
 ("Put This On","https://putthison.com/feed"),("Kicks on Fire","https://www.kicksonfire.com/feed/"),("Dazed","https://www.dazeddigital.com/rss"),
])
add("travel", [
 ("Condé Nast Traveler","https://www.cntraveler.com/feed/rss"),("Travel + Leisure","https://www.travelandleisure.com/feed"),("Lonely Planet","https://www.lonelyplanet.com/news/feed"),("The Points Guy","https://thepointsguy.com/feed/"),
 ("Skift","https://skift.com/feed/"),("Atlas Obscura","https://www.atlasobscura.com/feeds/latest"),("Guardian Travel","https://www.theguardian.com/travel/rss"),("NYT Travel","https://rss.nytimes.com/services/xml/rss/nyt/Travel.xml"),
 ("Matador Network","https://matadornetwork.com/feed/"),("AFAR","https://www.afar.com/feed"),("One Mile at a Time","https://onemileatatime.com/feed/"),("Nomadic Matt","https://www.nomadicmatt.com/feed/"),
 ("Fodor's","https://www.fodors.com/news/feed"),("Rick Steves","https://www.ricksteves.com/rss"),("Smarter Travel","https://www.smartertravel.com/feed/"),("View from the Wing","https://viewfromthewing.com/feed/"),
])
add("food", [
 ("Serious Eats","https://www.seriouseats.com/feed/serious-eats"),("Bon Appétit","https://www.bonappetit.com/feed/rss"),("Eater","https://www.eater.com/rss/index.xml"),("Food52","https://food52.com/blog.rss"),
 ("Smitten Kitchen","https://smittenkitchen.com/feed/"),("Budget Bytes","https://www.budgetbytes.com/feed/"),("Delish","https://www.delish.com/rss/all.xml/"),("Minimalist Baker","https://minimalistbaker.com/feed/"),
 ("Pinch of Yum","https://pinchofyum.com/feed"),("NYT Food","https://rss.nytimes.com/services/xml/rss/nyt/DiningandWine.xml"),("Just One Cookbook","https://www.justonecookbook.com/feed/"),("Love and Lemons","https://www.loveandlemons.com/feed/"),
 ("The Kitchn","https://www.thekitchn.com/main.rss"),("Epicurious","https://www.epicurious.com/feed/rss"),("Sally's Baking","https://sallysbakingaddiction.com/feed/"),("Cookie and Kate","https://cookieandkate.com/feed/"),
])
add("auto", [
 ("Motor1","https://www.motor1.com/rss/articles/all/"),("Autoblog","https://www.autoblog.com/rss.xml"),("Car and Driver","https://www.caranddriver.com/rss/all.xml/"),("Jalopnik","https://jalopnik.com/rss"),
 ("Road & Track","https://www.roadandtrack.com/rss/all.xml/"),("Electrek","https://electrek.co/feed/"),("MotorTrend","https://www.motortrend.com/feed/"),("Autocar","https://www.autocar.co.uk/rss"),
 ("Carscoops","https://www.carscoops.com/feed/"),("Teslarati","https://www.teslarati.com/feed/"),("InsideEVs","https://insideevs.com/rss/articles/all/"),("Hagerty","https://www.hagerty.com/media/feed/"),
 ("Top Gear","https://www.topgear.com/feeds/rss/news"),("The Drive","https://www.thedrive.com/feed"),("Bring a Trailer","https://bringatrailer.com/feed/"),("CleanTechnica EVs","https://cleantechnica.com/category/evs/feed/"),
])
add("home", [
 ("Apartment Therapy","https://www.apartmenttherapy.com/main.rss"),("The Spruce","https://www.thespruce.com/rss"),("Curbed","https://www.curbed.com/rss/index.xml"),("Remodelista","https://www.remodelista.com/feed/"),
 ("Gardenista","https://www.gardenista.com/feed/"),("Dwell","https://www.dwell.com/feed"),("Houzz","https://www.houzz.com/magazine/feed"),("This Old House","https://www.thisoldhouse.com/feeds/all"),
 ("Hunker","https://www.hunker.com/feed"),("Better Homes & Gardens","https://www.bhg.com/feed/"),("Domino","https://www.domino.com/rss"),("Young House Love","https://www.younghouselove.com/feed/"),
 ("Lifehacker","https://lifehacker.com/feed/rss"),("Real Simple","https://www.realsimple.com/feed"),("Gardening Know How","https://www.gardeningknowhow.com/feed"),
])
add("celebrity", [
 ("TMZ","https://www.tmz.com/rss.xml"),("People","https://people.com/feed/"),("E! News","https://www.eonline.com/syndication/feeds/rssfeeds/topstories.xml"),("Entertainment Weekly","https://ew.com/feed/"),
 ("Page Six","https://pagesix.com/feed/"),("Just Jared","https://www.justjared.com/feed/"),("Perez Hilton","https://perezhilton.com/feed/"),("BuzzFeed","https://www.buzzfeed.com/index.xml"),
 ("Daily Mail Showbiz","https://www.dailymail.co.uk/tvshowbiz/index.rss"),("Us Weekly","https://www.usmagazine.com/feed/"),("Billboard","https://www.billboard.com/feed/"),("Deadline","https://deadline.com/feed/"),
 ("Glamour","https://www.glamour.com/feed/rss"),("Vanity Fair","https://www.vanityfair.com/feed/rss"),("The Cut","https://www.thecut.com/rss/index.xml"),
])
add("health", [
 ("Harvard Health","https://www.health.harvard.edu/blog/feed"),("NPR Health","https://feeds.npr.org/1128/rss.xml"),("Medical News Today","https://www.medicalnewstoday.com/rss/medicalnewstoday.xml"),
 ("STAT News","https://www.statnews.com/feed/"),("NYT Health","https://rss.nytimes.com/services/xml/rss/nyt/Health.xml"),("Guardian Health","https://www.theguardian.com/society/health/rss"),
 ("Men's Health","https://www.menshealth.com/rss/all.xml/"),("Women's Health","https://www.womenshealthmag.com/rss/all.xml/"),("Science-Based Medicine","https://sciencebasedmedicine.org/feed/"),
 ("WebMD","https://rssfeeds.webmd.com/rss/rss.aspx?RSSSource=RSS_PUBLIC"),("Mayo Clinic News","https://newsnetwork.mayoclinic.org/feed/"),("Healthline","https://www.healthline.com/rss/health-news"),
 ("Psychology Today","https://www.psychologytoday.com/us/rss.xml"),("Runner's World","https://www.runnersworld.com/rss/all.xml/"),("Examine","https://examine.com/feed/"),("KFF Health News","https://kffhealthnews.org/feed/"),
])
add("climate", [
 ("Grist","https://grist.org/feed/"),("Guardian Environment","https://www.theguardian.com/environment/rss"),("Inside Climate News","https://insideclimatenews.org/feed/"),("Carbon Brief","https://www.carbonbrief.org/feed/"),
 ("Mongabay","https://news.mongabay.com/feed/"),("Yale E360","https://e360.yale.edu/feed.xml"),("CleanTechnica","https://cleantechnica.com/feed/"),("NPR Climate","https://feeds.npr.org/1167/rss.xml"),
 ("NASA Climate","https://climate.nasa.gov/news/rss.xml"),("Earth.org","https://earth.org/feed/"),("Treehugger","https://www.treehugger.com/feeds/rss"),("Canary Media","https://www.canarymedia.com/feeds/rss"),
 ("Electrek","https://electrek.co/feed/"),("Heatmap","https://heatmap.news/feeds/news.xml"),("The Revelator","https://therevelator.org/feed/"),
])
add("culture", [
 ("The New Yorker","https://www.newyorker.com/feed/everything"),("Longreads","https://longreads.com/feed/"),("Literary Hub","https://lithub.com/feed/"),("Open Culture","https://www.openculture.com/feed"),
 ("Kottke.org","https://feeds.kottke.org/main"),("The Marginalian","https://www.themarginalian.org/feed/"),("Mental Floss","https://www.mentalfloss.com/rss.xml"),("The Onion","https://theonion.com/rss"),
 ("xkcd","https://xkcd.com/rss.xml"),("Aeon","https://aeon.co/feed.rss"),("The Conversation","https://theconversation.com/global/articles.atom"),("Nautilus","https://nautil.us/feed/"),
 ("Slate","https://slate.com/feeds/all.rss"),("The Guardian Culture","https://www.theguardian.com/culture/rss"),("Wait But Why","https://waitbutwhy.com/feed"),("Quartz","https://qz.com/rss"),
 ("Hyperallergic","https://hyperallergic.com/feed/"),("Public Domain Review","https://publicdomainreview.org/rss.xml"),("Smithsonian","https://www.smithsonianmag.com/rss/latest_articles/"),
])
