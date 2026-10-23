package de.gwerlight.xgwo.fileedit.desktop.spec

import java.nio.file.Path
import java.nio.file.Paths

/** Die eigenen Minimal-Dateien aus {@code src/test/resources/spec}: kleines Schema, kleine Regel und passende Nachrichten. */
class SpecFixtures {

    static final String NS = 'urn:fileedit:test:1.0'

    static Path xsd() { resource('/spec/mini.xsd') }

    static Path xsl() { resource('/spec/mini-rules.xsl') }

    static Specification mini(String version = '1.0') {
        new Specification('Mini', version, xsd(), xsl())
    }

    /** Gültige Nachricht: alle Beträge sind positiv. */
    static String valid() {
        """<?xml version="1.0" encoding="UTF-8"?>
<t:rechnung xmlns:t="${NS}">
    <t:betrag>10.50</t:betrag>
</t:rechnung>"""
    }

    /** Verletzt die Regel TEST-0001 im zweiten Betrag (Zeile 4). */
    static String negativeAmount() {
        """<?xml version="1.0" encoding="UTF-8"?>
<t:rechnung xmlns:t="${NS}">
    <t:betrag>10.50</t:betrag>
    <t:betrag>-3</t:betrag>
</t:rechnung>"""
    }

    /** Verletzt das Schema: Der Betrag ist keine Zahl. */
    static String notANumber() {
        """<?xml version="1.0" encoding="UTF-8"?>
<t:rechnung xmlns:t="${NS}">
    <t:betrag>viel</t:betrag>
</t:rechnung>"""
    }

    private static Path resource(String name) {
        Paths.get(SpecFixtures.getResource(name).toURI())
    }
}
