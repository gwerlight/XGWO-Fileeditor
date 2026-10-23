package de.gwerlight.xgwo.fileedit.desktop.config;

import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle;

/**
 * Eintrag der Schema-Auswahl.
 *
 * @param label  Anzeigetext
 * @param bundle fest gewähltes Schema oder {@code null} für die automatische Erkennung
 */
public record SchemaOption(String label, SchemaBundle bundle) {

    public static SchemaOption automatic() {
        return new SchemaOption("Automatisch erkennen", null);
    }

    @Override
    public String toString() {
        return label;
    }
}
