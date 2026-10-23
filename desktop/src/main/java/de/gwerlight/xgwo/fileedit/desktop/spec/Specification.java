package de.gwerlight.xgwo.fileedit.desktop.spec;

import java.nio.file.Path;

/**
 * Eine vom Anwender eingerichtete Spezifikation. Die Dateien gehören nicht zur Anwendung, es werden nur ihre Pfade gemerkt.
 *
 * @param name    Anzeigename der Spezifikation, z. B. „XGWO“
 * @param version Version, z. B. „1.6“ (darf leer sein)
 * @param xsd     Haupt-XSD; Includes und Imports werden relativ dazu aufgelöst
 * @param xsl     vorkompilierte Schematron-Regeln (XSLT), gehören zwingend zur Spezifikation
 */
public record Specification(String name, String version, Path xsd, Path xsl) {

    /** Text für Auswahlfelder und Meldungen, z. B. „XGWO 1.6“. */
    public String label() {
        return version == null || version.isBlank() ? name : name + " " + version;
    }
}
