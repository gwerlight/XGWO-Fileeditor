package de.gwerlight.xgwo.fileedit.core.message;

import de.gwerlight.xgwo.fileedit.core.model.Origin;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Übersetzt die häufigsten XSD-Fehlercodes (Xerces "cvc-…") in verständliche deutsche Texte.
 * Der Code steht unabhängig von der JVM-Sprache am Anfang der Meldung und ist damit stabil.
 */
public final class DefaultMessageTranslator implements MessageTranslator {

    private static final Pattern CODE = Pattern.compile("^(cvc-[A-Za-z0-9.\\-]+)");

    private static final Map<String, String> TEXTS = Map.ofEntries(
            Map.entry("cvc-complex-type.2.4.a", "An dieser Stelle steht ein unerwartetes Element, oder ein erwartetes Element fehlt."),
            Map.entry("cvc-complex-type.2.4.b", "In diesem Element fehlen Pflichtelemente."),
            Map.entry("cvc-complex-type.2.4.d", "Hier ist kein weiteres Element erlaubt."),
            Map.entry("cvc-complex-type.3.2.2", "Dieses Attribut ist hier nicht erlaubt."),
            Map.entry("cvc-complex-type.4", "Ein Pflichtattribut fehlt."),
            Map.entry("cvc-datatype-valid.1.2.1", "Der Wert hat nicht das erwartete Format."),
            Map.entry("cvc-enumeration-valid", "Der Wert gehört nicht zu den erlaubten Werten."),
            Map.entry("cvc-pattern-valid", "Der Wert entspricht nicht dem geforderten Muster."),
            Map.entry("cvc-minLength-valid", "Der Wert ist zu kurz."),
            Map.entry("cvc-maxLength-valid", "Der Wert ist zu lang."),
            Map.entry("cvc-elt.1.a", "Das Wurzelelement ist im Schema nicht deklariert."));

    @Override
    public String translate(Origin origin, String ruleId, String technicalMessage) {
        if (origin == Origin.WELL_FORMED) {
            return "Das XML ist nicht wohlgeformt (z. B. fehlt ein schließendes Tag oder ein Zeichen ist ungültig).";
        }
        String code = ruleId;
        if (code == null && technicalMessage != null) {
            Matcher m = CODE.matcher(technicalMessage);
            if (m.find()) {
                code = m.group(1);
            }
        }
        String text = code == null ? null : TEXTS.get(code);
        if (text == null) {
            return technicalMessage;
        }
        // Die Originalmeldung enthält die konkreten Namen/Werte (Element, Wert, erwartete Elemente) und bleibt als Detail erhalten
        String detail = technicalMessage == null ? "" : technicalMessage.replaceFirst("^" + Pattern.quote(code) + ":\\s*", "");
        return detail.isBlank() ? text : text + " (" + detail + ")";
    }
}
