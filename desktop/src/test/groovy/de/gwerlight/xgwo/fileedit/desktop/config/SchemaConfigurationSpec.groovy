package de.gwerlight.xgwo.fileedit.desktop.config

import de.gwerlight.xgwo.fileedit.core.model.Origin
import de.gwerlight.xgwo.fileedit.desktop.spec.SpecFixtures
import de.gwerlight.xgwo.fileedit.desktop.spec.Specification

import java.nio.file.Files
import java.nio.file.Path

import spock.lang.Specification as SpockSpec
import spock.lang.TempDir

/** Prüft den Aufbau von Auswahl und Prüfdienst aus eingerichteten Spezifikationen, mit den eigenen Minimal-Dateien. */
class SchemaConfigurationSpec extends SpockSpec {

    @TempDir
    Path dir

    def "ohne Spezifikationen gibt es nur die automatische Erkennung"() {
        when:
        def config = SchemaConfiguration.from([])

        then:
        config.options().size() == 1
        config.options()[0].bundle() == null
        config.problems().isEmpty()
    }

    def "jede eingerichtete Spezifikation wird zur Auswahl"() {
        when:
        def config = SchemaConfiguration.from([SpecFixtures.mini('1.0')])

        then:
        config.options()*.label() == ['Automatisch erkennen', 'Mini 1.0']
        config.options()[1].bundle().id() == 'Mini 1.0'
        config.options()[1].bundle().schematron().size() == 1
        config.problems().isEmpty()
    }

    def "eine gültige Nachricht wird über den Namespace erkannt und besteht"() {
        given:
        def service = SchemaConfiguration.from([SpecFixtures.mini()]).service()

        when:
        def result = service.check(SpecFixtures.valid())

        then:
        result.valid
        result.schemaId() == 'Mini 1.0'
    }

    def "die fachliche Regel aus der eingerichteten XSL wird angewendet, samt Zeile"() {
        given:
        def service = SchemaConfiguration.from([SpecFixtures.mini()]).service()

        when:
        def result = service.check(SpecFixtures.negativeAmount())

        then:
        !result.valid
        def finding = result.findings().find { it.origin() == Origin.SCHEMATRON }
        finding.ruleId() == 'TEST-0001'
        finding.message() == 'Der Betrag muss größer als 0 sein.'
        finding.line() == 4
    }

    def "eine Verletzung des eingerichteten Schemas wird gemeldet"() {
        given:
        def service = SchemaConfiguration.from([SpecFixtures.mini()]).service()

        when:
        def result = service.check(SpecFixtures.notANumber())

        then:
        !result.valid
        result.findings().any { it.origin() == Origin.XSD }
    }

    def "ein Dokument mit unbekanntem Namespace bekommt eine verständliche Meldung"() {
        given:
        def service = SchemaConfiguration.from([SpecFixtures.mini()]).service()

        when:
        def result = service.check('<a xmlns="urn:unbekannt"/>')

        then:
        !result.valid
        result.findings()[0].origin() == Origin.SYSTEM
        result.findings()[0].message().contains("kein Schema")
    }

    def "eine unbrauchbare Spezifikation wird übersprungen und mit Grund gemeldet"() {
        given:
        def kaputt = new Specification('Kaputt', '9', Path.of('fehlt.xsd'), SpecFixtures.xsl())

        when:
        def config = SchemaConfiguration.from([kaputt, SpecFixtures.mini()])

        then:
        config.options()*.label() == ['Automatisch erkennen', 'Mini 1.0']
        config.problems().size() == 1
        config.problems()[0].startsWith('Kaputt 9: ')
    }

    def "bei gleichem Namespace erkennt die automatische Erkennung die erste, die zweite bleibt wählbar"() {
        given:
        def second = new Specification('Zweite', '1.0', SpecFixtures.xsd(), SpecFixtures.xsl())

        when:
        def config = SchemaConfiguration.from([SpecFixtures.mini('1.0'), second])
        def result = config.service().check(SpecFixtures.valid())

        then:
        config.options()*.label() == ['Automatisch erkennen', 'Mini 1.0', 'Zweite 1.0']
        result.schemaId() == 'Mini 1.0'
        config.problems().size() == 1
        config.problems()[0].contains("'Mini 1.0'")
    }

    def "eine geänderte XSL wirkt ohne Neustart der Anwendung"() {
        given: 'eine eigene Kopie der Regel, die später so geändert wird, dass sie nie meldet'
        def xsl = dir.resolve('regel.xsl')
        Files.copy(SpecFixtures.xsl(), xsl)
        def service = SchemaConfiguration.from([new Specification('Mini', '1.0', SpecFixtures.xsd(), xsl)]).service()
        assert !service.check(SpecFixtures.negativeAmount()).valid

        when:
        Files.writeString(xsl, Files.readString(xsl).replace('le 0', 'lt -1000'))
        Files.setLastModifiedTime(xsl, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 5000))

        then:
        service.check(SpecFixtures.negativeAmount()).valid
    }
}
