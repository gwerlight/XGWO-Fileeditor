package de.gwerlight.xgwo.fileedit.core

import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

/**
 * Die eigenen Minimal-Dateien aus {@code src/test/resources/spec}: ein kleines Schema (mit Include), drei Regeln und
 * gültige Beispielnachrichten. Sie prüfen nur die Grundfunktion und haben mit einer echten Spezifikation nichts zu tun.
 */
class Mini {

    static final String NS = 'urn:fileedit:test:1'

    static URL xsd() { Mini.getResource('/spec/mini.xsd') }

    static URL rules() { Mini.getResource('/spec/mini-rules.xsl') }

    /** Schema samt Regeln; ohne Regeln läuft nur die XSD-Prüfung. */
    static SchemaBundle bundle(boolean withRules = true) {
        new SchemaBundle('Mini 1.0', xsd(), withRules ? [rules()] : [])
    }

    static String message(String name) {
        Mini.getResource("/spec/messages/valid/${name}").getText('UTF-8')
    }

    static List<String> validFiles() {
        Path dir = Paths.get(Mini.getResource('/spec/messages/valid').toURI())
        Files.list(dir).withCloseable { s ->
            s.map { it.fileName.toString() }.filter { it.endsWith('.xml') }.sorted().collect(Collectors.toList())
        }
    }

    /** Zeilennummer (1-basiert) der ersten Zeile, die den Text enthält. */
    static int lineOf(String xml, String text) {
        xml.readLines().findIndexOf { it.contains(text) } + 1
    }
}
