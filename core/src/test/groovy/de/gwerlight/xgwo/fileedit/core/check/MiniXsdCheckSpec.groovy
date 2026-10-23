package de.gwerlight.xgwo.fileedit.core.check

import de.gwerlight.xgwo.fileedit.core.Mini
import de.gwerlight.xgwo.fileedit.core.model.Origin
import de.gwerlight.xgwo.fileedit.core.schema.NamespaceSchemaCatalog
import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle
import de.gwerlight.xgwo.fileedit.core.service.DefaultXmlCheckService

import java.nio.file.Files
import java.nio.file.Path

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

/** XSD-Prüfung mit dem eigenen Minimal-Schema (siehe {@link Mini}). */
class MiniXsdCheckSpec extends Specification {

    @Shared
    def service = DefaultXmlCheckService.withDefaults(new NamespaceSchemaCatalog([:]))

    @Shared
    String base = Mini.message('rechnung-einfach.xml')

    @TempDir
    Path tempDir

    @Unroll
    def "gültige Nachricht #file besteht die XSD-Prüfung"() {
        when:
        def result = service.check(Mini.message(file), Mini.bundle(false))

        then:
        result.valid
        result.schemaId() == 'Mini 1.0'
        result.findings().isEmpty()

        where:
        file << Mini.validFiles()
    }

    @Unroll
    def "#fall wird mit Code #code und Position gemeldet"() {
        when:
        def result = service.check(xml, Mini.bundle(false))
        def finding = result.findings().find { it.ruleId() == code }

        then:
        !result.valid
        finding != null
        finding.origin() == Origin.XSD
        finding.hasPosition()
        finding.message().startsWith(text)
        finding.technicalMessage().startsWith(code)

        where:
        fall                      | code                        | text                                     | xml
        'unbekanntes Attribut'    | 'cvc-complex-type.3.2.2'    | 'Dieses Attribut ist hier nicht erlaubt' | base.replace('standard="Mini"', 'standard="Mini" unbekannt="x"')
        'unbekanntes Kindelement' | 'cvc-complex-type.2.4.a'    | 'An dieser Stelle steht ein unerwartetes' | base.replace('<t:kopf>', '<unbekannt/><t:kopf>')
        'fehlendes Pflichtelement' | 'cvc-complex-type.2.4.b'   | 'In diesem Element fehlen Pflichtelemente' | base.replaceAll('<t:datum>.*</t:datum>', '')
        'fehlendes Pflichtattribut' | 'cvc-complex-type.4'      | 'Ein Pflichtattribut fehlt'              | base.replace(' art="ware"', '')
        'Wert außerhalb der Aufzählung' | 'cvc-enumeration-valid' | 'Der Wert gehört nicht zu den erlaubten' | base.replace('art="ware"', 'art="sonstiges"')
        'Verstoß gegen das Muster' | 'cvc-pattern-valid'       | 'Der Wert entspricht nicht dem geforderten' | base.replace('AB-1234', 'XY')
        'ungültiges Datum'        | 'cvc-datatype-valid.1.2.1'  | 'Der Wert hat nicht das erwartete Format' | base.replace('2026-01-31', 'gestern')
    }

    def "Folgefehler zur selben Ursache werden nicht zusätzlich gemeldet"() {
        when: 'ein Wert verletzt den Typ, Xerces meldet dazu noch einen Folgefehler derselben Zeile'
        def result = service.check(base.replace('AB-1234', 'XY'), Mini.bundle(false))

        then:
        result.findings().size() == 1
        result.findings()[0].line() == Mini.lineOf(base, 'AB-1234')
    }

    def "ein im Schema nicht deklariertes Wurzelelement wird gemeldet"() {
        when:
        def result = service.check("""<t:unbekannt xmlns:t="${Mini.NS}"/>""", Mini.bundle(false))

        then:
        result.findings()*.ruleId() == ['cvc-elt.1.a']
    }

    def "nicht wohlgeformtes XML wird als solches gemeldet, weitere Prüfungen entfallen"() {
        when:
        def result = service.check(base.replace('</t:rechnung>', ''), Mini.bundle())

        then:
        !result.valid
        result.findings().size() == 1
        result.findings()[0].origin() == Origin.WELL_FORMED
        result.findings()[0].message().contains('nicht wohlgeformt')
    }

    def "ein fehlerhaftes Schema wird als Systemfehler gemeldet, nicht als Befund"() {
        given:
        def broken = new SchemaBundle('Kaputt', tempDir.resolve('gibt-es-nicht.xsd').toUri().toURL(), [])

        when:
        service.check(base, broken)

        then:
        def e = thrown(IllegalStateException)
        e.message.contains("'Kaputt'")
    }

    def "externe Entitäten (XXE) werden nicht aufgelöst, der Dateiinhalt gelangt nicht in die Befunde"() {
        given:
        Path secret = tempDir.resolve('geheim.txt')
        Files.writeString(secret, 'GEHEIMER-INHALT')
        String xml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE x [<!ENTITY xxe SYSTEM "${secret.toUri()}">]>
<x>&xxe;</x>"""

        when:
        def result = service.check(xml, Mini.bundle(false))

        then:
        noExceptionThrown()
        !result.valid
        result.findings().every { !it.toString().contains('GEHEIMER-INHALT') }
    }
}
