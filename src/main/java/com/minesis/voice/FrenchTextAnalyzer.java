package com.minesis.voice;

import java.util.*;

/**
 * Lightweight French NLP for voice-clip semantic matching.
 *
 * No external dependencies, no model downloads.  Three layers:
 *  1. Stopword removal — discards grammatical noise (articles, pronouns…)
 *  2. Suffix normalization — collapses morphological variants to a root form
 *     ("courait", "courus", "courions" → "cour"; "mines", "miné" → "min")
 *  3. Synonym expansion — groups semantically equivalent Minecraft/social words so
 *     "dépêche" matches "cours", "vite", "sprint", etc.
 *
 * Similarity is Jaccard on the union of stems and their synonym groups, with exact
 * stem matches weighted higher (1.0) than synonym-only matches (0.7).
 */
public class FrenchTextAnalyzer {

    // ─── Stopwords ────────────────────────────────────────────────────────

    private static final Set<String> STOPS = new HashSet<>(Arrays.asList(
        // articles & determiners
        "le","la","les","un","une","des","du","de","l","d","au","aux",
        // personal pronouns
        "je","tu","il","elle","on","nous","vous","ils","elles",
        "me","te","se","lui","y","en","moi","toi","soi",
        // conjunctions
        "et","ou","mais","donc","or","ni","car","que","quand","si",
        // prepositions
        "a","à","en","dans","sur","sous","par","pour","avec","sans","entre","vers","chez",
        "avant","après","pendant","depuis","jusque","jusqu",
        // auxiliaries & copula
        "est","sont","était","étaient","être","avoir","ai","as","ont","avait","avaient",
        "sera","seront","aurait","auraient","fait","font",
        // demonstratives & possessives
        "ce","cet","cette","ces","mon","ton","son","ma","ta","sa","notre","votre","leur","leurs",
        // adverbs & filler
        "pas","ne","plus","très","bien","aussi","même","tout","tous","toutes","trop",
        "là","ici","voilà","alors","vraiment","déjà","encore","jamais","toujours",
        // short function words (handled after apostrophe split)
        "c","s","n","m","t","j","qu","l","d"
    ));

    // ─── Synonym groups ───────────────────────────────────────────────────
    //
    // Each group is a set of STEMS (already normalized by stem()).
    // Order doesn't matter; membership means "semantically equivalent here".

    private static final String[][] SYNONYM_GROUPS = {
        // ── Movement ──────────────────────────────────────────────────────
        { "cour","sprint","vite","rapid","dépêch","fuir","fui","rush","vit" },   // running / hurry
        { "march","avanc","bou","vien","all","partir","part","vai","boug" },     // walking / coming
        { "nag","plong","baign","swim" },                                         // swimming
        { "vol","fly","plane" },                                                  // flying
        { "sauter","saut","jump","bondir","bond" },                              // jumping
        { "ramp","crawl","ramper" },                                              // crawling

        // ── Minecraft actions ─────────────────────────────────────────────
        { "min","creus","cass","chop","pioch","bris" },                          // mining / breaking
        { "constru","build","bât","bâtis","bâti","craft","fabriqu","crée","construi","construire" },
        { "récolt","récolter","harvest","ramass","ramasss","collect" },          // harvesting
        { "plant","cultiv","farm","semer","sem" },                               // farming
        { "cuir","cuison","cook","cuisin","smelt","fondr" },                     // cooking / smelting
        { "enchant","forge","répar","repare","repair","anvil","enclume" },       // enchanting/repair
        { "chass","hunt","traqu","track","pist" },                               // hunting
        { "pêch","fish","pêcher","pêcheur" },                                    // fishing
        { "explo","explor","cherch","chercher","découvr" },                      // exploring
        { "cavalier","monter","mont","cheval","ride" },                          // riding

        // ── Combat ────────────────────────────────────────────────────────
        { "attaqu","frapp","bat","tuer","tue","frappe","combat","fight","batt","battr","slash" },
        { "bloqu","défend","defens","shield","écu","bloquant" },                 // defending
        { "fui","esquiv","dodge","esquiver","évit" },                            // dodging
        { "tir","shoot","arc","flèch","arroser","arrow" },                       // ranged

        // ── Social / greetings ────────────────────────────────────────────
        { "bonjour","salut","coucou","hey","bonsoir","allo","allô","hello","bjr","hi" },
        { "aurevoir","ciao","byebye","bonne journée","tchao","plus","départ" },  // goodbye
        { "merci","thanks","remerci","gratitud","thx" },
        { "oui","ouais","ok","bien","ouep","d'accord","daccord","yep","yes" },   // agreement
        { "non","nope","hein","jamais","nan","nope" },                           // disagreement
        { "aide","help","secours","sos","sauv","aider" },                        // help
        { "pardon","désolé","sorry","excuse","mea culpa" },                      // apology
        { "bravo","génial","super","cool","nickel","top","bien joué","gg" },     // praise

        // ── State / emotion ───────────────────────────────────────────────
        { "fatigué","épuis","crevé","mort","mour","mourir","fatigu" },           // tired / dead
        { "faim","manger","bouffe","food","pain","viande","hunger","mang" },     // hungry
        { "soif","boire","eau","water","drink" },                                // thirsty
        { "peur","trouille","effray","scared","craign","craint","afraid" },      // scared
        { "bless","mal","douleur","hurt","souffr","souffrir","dommag" },        // hurt
        { "fort","puissant","strong","power","powerful","force" },               // strong
        { "perdu","lost","où suis","où es","trouv","retrouv" },                 // lost

        // ── Inventory / items ─────────────────────────────────────────────
        { "épée","sword","couteau","blade","lame","sabre" },
        { "arc","bow","arbalète","crossbow" },
        { "bouclier","shield","écu","protéc" },
        { "armure","casque","cuirasse","jambier","botte","armor","helm","chest","leg","boot" },
        { "hache","axe","pioch","pickaxe","pelle","shovel","houe","hoe" },
        { "maison","base","home","abri","chez moi","village","camp" },           // base / home
        { "diamant","diamond","emeraude","emerald","or","gold","iron","fer","netherit" },

        // ── Question / location ───────────────────────────────────────────
        { "où","ici","là","localisat","direct","localisation","droite","gauche","devant","derrière" },
        { "quand","moment","temps","heure","soon","bientôt","maintenant","now" },
        { "pourquoi","raison","cause","parce","because" },
        { "comment","façon","manière","maniére","méthode","how" },
    };

    // word → set of synonyms (built once at class load)
    private static final Map<String, Set<String>> SYNONYM_LOOKUP = new HashMap<>();
    static {
        for (String[] group : SYNONYM_GROUPS) {
            Set<String> s = new HashSet<>(Arrays.asList(group));
            for (String word : group) SYNONYM_LOOKUP.put(word, s);
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────

    /**
     * Semantic similarity in [0, 1] between two French phrases.
     *
     * Exact stem match → weight 1.0
     * Synonym-only match → weight 0.7
     * No match → 0
     *
     * Uses weighted Jaccard: Σ(weights of shared tokens) / |union of expanded sets|
     */
    public static float similarity(String a, String b) {
        if (isNullOrBlank(a) || isNullOrBlank(b)) return 0f;
        List<String> stemsA = tokenize(a);
        List<String> stemsB = tokenize(b);
        if (stemsA.isEmpty() || stemsB.isEmpty()) return 0f;

        Set<String> setA = new HashSet<>(stemsA);
        Set<String> setB = new HashSet<>(stemsB);

        float numerator = 0f;

        // Exact stem matches
        Set<String> exact = new HashSet<>(setA);
        exact.retainAll(setB);
        numerator += exact.size() * 1.0f;

        // Synonym matches (only for tokens NOT in exact)
        Set<String> onlyA = new HashSet<>(setA); onlyA.removeAll(exact);
        Set<String> onlyB = new HashSet<>(setB); onlyB.removeAll(exact);
        Set<String> synonymMatchedB = new HashSet<>();
        for (String wa : onlyA) {
            Set<String> group = SYNONYM_LOOKUP.get(wa);
            if (group == null) continue;
            for (String wb : onlyB) {
                if (!synonymMatchedB.contains(wb) && group.contains(wb)) {
                    numerator += 0.7f;
                    synonymMatchedB.add(wb);
                    break;
                }
            }
        }

        // Expanded union size (stems + their synonym group members)
        Set<String> expandedA = expand(setA);
        Set<String> expandedB = expand(setB);
        Set<String> union = new HashSet<>(expandedA);
        union.addAll(expandedB);
        int denominator = union.size();
        if (denominator == 0) return 0f;

        return Math.min(1.0f, numerator / denominator);
    }

    /**
     * Tokenizes French text: lowercase → split → remove stopwords → stem.
     * Returns the list of meaningful stems.
     */
    public static List<String> tokenize(String text) {
        if (isNullOrBlank(text)) return Collections.emptyList();
        String low = text.toLowerCase(Locale.ROOT)
                // keep letters, accented chars, apostrophes, hyphens, spaces
                .replaceAll("[^a-zàâäéèêëîïôöùûüÿœæçœ'\\-\\s]", " ")
                .replaceAll("[''']", " ")  // expand apostrophes → split "j'ai" into "j" "ai"
                .replaceAll("\\s+", " ").trim();

        List<String> result = new ArrayList<>();
        for (String raw : low.split("[\\s\\-]+")) {
            String w = raw.replaceAll("^[\\-]+|[\\-]+$", "").trim();
            if (w.length() < 2) continue;
            if (STOPS.contains(w)) continue;
            String s = stem(w);
            if (s.length() >= 2 && !STOPS.contains(s)) result.add(s);
        }
        return result;
    }

    /** Returns the synonym group for a given stem, or null if none. */
    public static Set<String> synonymGroup(String stem) {
        return SYNONYM_LOOKUP.get(stem);
    }

    // ─── Suffix normalization ─────────────────────────────────────────────
    //
    // Rules applied longest-first to avoid partial stripping.
    // Only strips when the remaining root is >= 3 chars (avoids over-stemming).

    static String stem(String w) {
        // ── Verb/adverb long suffixes ──────────────────────────────────────
        if (tryStrip(w, "issement", 4) != null) return tryStrip(w, "issement", 4);
        if (tryStrip(w, "issement", 4) != null) return tryStrip(w, "issement", 4);
        if (tryStrip(w, "ements",   4) != null) return tryStrip(w, "ements",   4);
        if (tryStrip(w, "ement",    4) != null) return tryStrip(w, "ement",    4);
        if (tryStrip(w, "aient",    3) != null) return tryStrip(w, "aient",    3);
        if (tryStrip(w, "eront",    3) != null) return tryStrip(w, "eront",    3);
        if (tryStrip(w, "iront",    3) != null) return tryStrip(w, "iront",    3);
        if (tryStrip(w, "ions",     3) != null) return tryStrip(w, "ions",     3);
        if (tryStrip(w, "ons",      3) != null) return tryStrip(w, "ons",      3);
        if (tryStrip(w, "ant",      3) != null) return tryStrip(w, "ant",      3);
        if (tryStrip(w, "ait",      3) != null) return tryStrip(w, "ait",      3);
        if (tryStrip(w, "ais",      3) != null) return tryStrip(w, "ais",      3);
        if (tryStrip(w, "iez",      3) != null) return tryStrip(w, "iez",      3);
        if (tryStrip(w, "ez",       3) != null) return tryStrip(w, "ez",       3);
        // ── Past participle ────────────────────────────────────────────────
        if (tryStrip(w, "ées",      3) != null) return tryStrip(w, "ées",      3) + "é";
        if (tryStrip(w, "ée",       3) != null) return tryStrip(w, "ée",       3) + "é";
        if (tryStrip(w, "és",       3) != null) return tryStrip(w, "és",       3) + "é";
        // ── Infinitive endings ─────────────────────────────────────────────
        if (tryStrip(w, "eux",      3) != null) return tryStrip(w, "eux",      3);
        if (tryStrip(w, "aux",      3) != null) return tryStrip(w, "aux",      3) + "al";
        if (tryStrip(w, "eau",      3) != null) return tryStrip(w, "eau",      3);
        if (tryStrip(w, "ier",      3) != null) return tryStrip(w, "ier",      3);
        if (tryStrip(w, "ère",      3) != null) return tryStrip(w, "ère",      3);
        if (tryStrip(w, "er",       3) != null) return tryStrip(w, "er",       3);
        if (tryStrip(w, "ir",       3) != null) return tryStrip(w, "ir",       3);
        if (tryStrip(w, "re",       3) != null) return tryStrip(w, "re",       3);
        // ── Plural / feminine ─────────────────────────────────────────────
        if (tryStrip(w, "iques",    3) != null) return tryStrip(w, "iques",    3) + "ique";
        if (tryStrip(w, "ique",     3) != null) return tryStrip(w, "ique",     3);
        if (tryStrip(w, "ques",     3) != null) return tryStrip(w, "ques",     3) + "que";
        if (tryStrip(w, "eurs",     3) != null) return tryStrip(w, "eurs",     3) + "eur";
        if (tryStrip(w, "eur",      3) != null) return tryStrip(w, "eur",      3);
        if (tryStrip(w, "tion",     3) != null) return tryStrip(w, "tion",     3);
        if (tryStrip(w, "tes",      3) != null) return tryStrip(w, "tes",      3);
        if (tryStrip(w, "nes",      3) != null) return tryStrip(w, "nes",      3);
        if (tryStrip(w, "les",      3) != null) return tryStrip(w, "les",      3);
        if (tryStrip(w, "es",       3) != null) return tryStrip(w, "es",       3);
        if (tryStrip(w, "é",        3) != null) return tryStrip(w, "é",        3);
        if (w.length() > 3 && w.endsWith("s") && !w.endsWith("ss"))
            return w.substring(0, w.length()-1);
        return w;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    /** Strips suffix if remaining root length >= minRoot. Returns null if can't strip. */
    private static String tryStrip(String w, String suffix, int minRoot) {
        if (w.endsWith(suffix) && w.length() - suffix.length() >= minRoot)
            return w.substring(0, w.length() - suffix.length());
        return null;
    }

    private static Set<String> expand(Set<String> stems) {
        Set<String> out = new HashSet<>(stems);
        for (String s : stems) {
            Set<String> group = SYNONYM_LOOKUP.get(s);
            if (group != null) out.addAll(group);
        }
        return out;
    }

    private static boolean isNullOrBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
