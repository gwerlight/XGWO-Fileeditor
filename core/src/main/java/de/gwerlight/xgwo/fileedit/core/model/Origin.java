package de.gwerlight.xgwo.fileedit.core.model;

/** Woher ein Befund stammt. */
public enum Origin {
    /** XML ist nicht wohlgeformt (z. B. fehlendes schließendes Tag). */
    WELL_FORMED,
    /** Verstoß gegen die XSD. */
    XSD,
    /** Verstoß gegen eine Schematron-Regel (fachliche Prüfung). */
    SCHEMATRON,
    /** Konfigurations-/Systemhinweis, z. B. kein Schema für das Wurzelelement gefunden. */
    SYSTEM
}
