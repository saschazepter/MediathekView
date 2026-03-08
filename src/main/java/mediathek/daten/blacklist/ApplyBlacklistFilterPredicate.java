package mediathek.daten.blacklist;

import mediathek.config.MVConfig;
import mediathek.daten.DatenFilm;
import mediathek.tool.Filter;

import java.util.*;
import java.util.function.Predicate;

class ApplyBlacklistFilterPredicate implements Predicate<DatenFilm> {
    private static final String[] EMPTY_STRING = {""};
    private final boolean isWhitelist;
    private final List<CompiledBlacklistRule> globalRules;
    private final Map<String, List<CompiledBlacklistRule>> senderRuleIndex;

    public ApplyBlacklistFilterPredicate(ListeBlacklist listeBlacklist) {
        isWhitelist = Boolean.parseBoolean(MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_IST_WHITELIST));
        final var compiledRules = compileRules(listeBlacklist);
        globalRules = List.copyOf(compiledRules.globalRules());
        senderRuleIndex = Map.copyOf(compiledRules.senderRuleIndex());
    }

    private CompiledRules compileRules(ListeBlacklist listeBlacklist) {
        final var globalEntries = new ArrayList<CompiledBlacklistRule>(listeBlacklist.size());
        final var indexedEntries = new HashMap<String, List<CompiledBlacklistRule>>(listeBlacklist.size());
        for (BlacklistRule entry : listeBlacklist) {
            final var senderSuchen = entry.getSender().toLowerCase(Locale.getDefault());
            final var themaSuchen = entry.getThema().toLowerCase(Locale.getDefault());
            final var titelSuchen = createPattern(entry.hasTitlePattern(), normalizeRuleText(entry.getTitel(), entry.hasTitlePattern()));
            final var themaTitelSuchen = createPattern(entry.hasThemaPattern(), normalizeRuleText(entry.getThema_titel(), entry.hasThemaPattern()));
            final var compiledRule = new CompiledBlacklistRule(
                    senderSuchen,
                    themaSuchen,
                    titelSuchen,
                    themaTitelSuchen,
                    senderSuchen.isEmpty(),
                    themaSuchen.isEmpty(),
                    hasTerms(titelSuchen),
                    hasTerms(themaTitelSuchen));
            if (compiledRule.matchesAnySender) {
                globalEntries.add(compiledRule);
            } else {
                indexedEntries.computeIfAbsent(compiledRule.senderSuchen(), _ -> new ArrayList<>()).add(compiledRule);
            }
        }
        final var immutableIndex = new HashMap<String, List<CompiledBlacklistRule>>(indexedEntries.size());
        indexedEntries.forEach((sender, rules) -> immutableIndex.put(sender, List.copyOf(rules)));
        return new CompiledRules(List.copyOf(globalEntries), immutableIndex);
    }

    @Override
    public boolean test(DatenFilm film) {
        final var sender = film.getSender().toLowerCase(Locale.getDefault());
        final var thema = film.getThema().toLowerCase(Locale.getDefault());
        final var title = film.getTitle();

        if (matchesAnyRule(globalRules, sender, thema, title)) {
            return isWhitelist;
        }

        final var senderRules = senderRuleIndex.get(sender);
        if (senderRules != null && matchesAnyRule(senderRules, sender, thema, title)) {
            return isWhitelist;
        }

        //found nothing
        return !isWhitelist;
    }

    protected String[] mySplit(final String inputString) {
        final String[] pTitle = inputString.split(",");
        if (pTitle.length == 0)
            return EMPTY_STRING;
        else
            return pTitle;
    }

    private String[] createPattern(final boolean isPattern, final String inputString) {
        if (isPattern)
            return new String[]{inputString};
        else
            return mySplit(inputString);
    }

    private String normalizeRuleText(final String inputString, final boolean isPattern) {
        if (isPattern) {
            return inputString;
        }

        return inputString.toLowerCase(Locale.getDefault());
    }

    private boolean hasTerms(final String[] terms) {
        return terms.length > 0 && !terms[0].isEmpty();
    }

    private boolean matchesAnyRule(final List<CompiledBlacklistRule> rules,
                                   final String sender,
                                   final String thema,
                                   final String title) {
        for (CompiledBlacklistRule entry : rules) {
            if (performFiltering(entry, sender, thema, title)) {
                return true;
            }
        }
        return false;
    }

    private boolean performFiltering(final CompiledBlacklistRule entry,
                                     final String sender,
                                     final String thema,
                                     final String title) {
        // prüfen ob xxxSuchen im String imXxx enthalten ist, themaTitelSuchen wird mit Thema u. Titel verglichen
        // senderSuchen exakt mit sender
        // themaSuchen exakt mit thema
        // titelSuchen muss im Titel nur enthalten sein

        if (!entry.matchesAnySender && !sender.equals(entry.senderSuchen())) {
            return false;
        }
        if (!entry.matchesAnyThema && !thema.equals(entry.themaSuchen())) {
            return false;
        }
        if (entry.hasTitleTerms && !Filter.pruefen(entry.titelSuchen(), title)) {
            return false;
        }
        if (entry.hasThemaTitleTerms
                && !Filter.pruefen(entry.themaTitelSuchen(), thema)
                && !Filter.pruefen(entry.themaTitelSuchen(), title)) {
            return false;
        }
        return true;
    }

    private record CompiledBlacklistRule(
            String senderSuchen,
            String themaSuchen,
            String[] titelSuchen,
            String[] themaTitelSuchen,
            boolean matchesAnySender,
            boolean matchesAnyThema,
            boolean hasTitleTerms,
            boolean hasThemaTitleTerms) {
    }

    private record CompiledRules(
            List<CompiledBlacklistRule> globalRules,
            Map<String, List<CompiledBlacklistRule>> senderRuleIndex) {
    }
}
