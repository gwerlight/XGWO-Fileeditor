package de.gwerlight.xgwo.fileedit.core.model;

/**
 * Ein einzelner Prüfbefund. Bewusst UI-neutral: Desktop und später Web entscheiden selbst, wie er dargestellt wird.
 *
 * @param origin           Herkunft (Wohlgeformtheit, XSD, Schematron, System)
 * @param severity         Schweregrad
 * @param line             Zeile (1-basiert) oder -1, wenn unbekannt
 * @param column           Spalte (1-basiert) oder -1, wenn unbekannt
 * @param xpath            Ort im Dokument als XPath (z. B. bei Schematron) oder {@code null}
 * @param ruleId           technische Regel-/Fehlerkennung (z. B. "cvc-complex-type.2.4.a", Schematron-Rule-ID) oder {@code null}
 * @param message          Meldung in Fachsprache für den Support
 * @param technicalMessage Originalmeldung des Prüfers (für Entwickler)
 */
public record Finding(Origin origin,
                      Severity severity,
                      int line,
                      int column,
                      String xpath,
                      String ruleId,
                      String message,
                      String technicalMessage) {

    public boolean hasPosition() {
        return line > 0;
    }
}
