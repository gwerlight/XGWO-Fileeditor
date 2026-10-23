package de.gwerlight.xgwo.fileedit.core.schema;

import java.net.URL;
import java.util.List;

/**
 * Alles, was zu einer Meldungsart gehört: XSD und die Schematron-Regeln. Die Regeln liegen als vorkompilierte XSLT vor;
 * ohne Regeln läuft nur die XSD-Prüfung. Die Dateien gehören nicht zur Anwendung, sondern kommen von der jeweiligen
 * Spezifikation (der Desktop-Client lässt sie vom Anwender einrichten). URLs statt Pfade, damit dieselbe Logik später
 * auch mit Classpath-Ressourcen oder Volumes im Container läuft.
 */
public record SchemaBundle(String id, URL xsd, List<URL> schematron) {

    public SchemaBundle {
        schematron = List.copyOf(schematron);
    }
}
