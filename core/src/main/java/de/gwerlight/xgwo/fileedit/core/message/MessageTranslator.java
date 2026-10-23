package de.gwerlight.xgwo.fileedit.core.message;

import de.gwerlight.xgwo.fileedit.core.model.Origin;

/** Übersetzt technische Prüfmeldungen in Fachsprache. Erweiterungspunkt für projektspezifische Texte. */
public interface MessageTranslator {
    /** @return verständliche Meldung; wenn nichts Passendes bekannt ist, die technische Meldung unverändert */
    String translate(Origin origin, String ruleId, String technicalMessage);
}
