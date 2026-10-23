package de.gwerlight.xgwo.fileedit.core.check

import de.gwerlight.xgwo.fileedit.core.Mini
import de.gwerlight.xgwo.fileedit.core.model.Origin
import de.gwerlight.xgwo.fileedit.core.model.Severity
import de.gwerlight.xgwo.fileedit.core.schema.NamespaceSchemaCatalog
import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle
import de.gwerlight.xgwo.fileedit.core.service.DefaultXmlCheckService

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

/**
 * Fachliche Prüfung (Schematron als vorkompiliertes XSLT) mit den eigenen Minimal-Regeln aus {@link Mini}. Das
 * Stylesheet beginnt bewusst mit einem Kommentar vor dem Wurzelelement; die Zeilenermittlung muss trotzdem funktionieren.
 */
class MiniSchematronCheckSpec extends Specification {

    @Shared
    def service = DefaultXmlCheckService.withDefaults(new NamespaceSchemaCatalog([:]))

    @Shared
    String twoPositions = Mini.message('rechnung-mehrere-positionen.xml')

    @TempDir
    Path tempDir

    private static List schematron(result) {
        result.findings().findAll { it.origin() == Origin.SCHEMATRON }
    }

    @Unroll
    def "gültige Nachricht #file besteht auch die fachliche Prüfung"() {
        when:
        def result = service.check(Mini.message(file), Mini.bundle())

        then:
        result.valid
        schematron(result).isEmpty()

        where:
        file << Mini.validFiles()
    }

    def "Regelverstoß wird mit Regel-ID, Meldung, Position und XPath gemeldet"() {
        given:
        def xml = twoPositions.replace('<t:betrag>250.75</t:betrag>', '<t:betrag>-5</t:betrag>')

        when:
        def result = service.check(xml, Mini.bundle())
        def finding = schematron(result).find { it.ruleId() == 'TEST-0001' }

        then:
        !result.valid
        schematron(result).size() == 1
        finding.severity() == Severity.ERROR
        finding.message() == '[TEST-0001] Der Betrag muss größer als 0 sein.'
        finding.line() == Mini.lineOf(xml, '-5')
        finding.xpath() == '/t:rechnung/t:position[2]/t:betrag'
        finding.technicalMessage().startsWith('Regel nicht erfüllt')
    }

    def "mehrere Verstöße werden einzeln und mit eigener Position gemeldet"() {
        given:
        def xml = twoPositions.replace('<t:betrag>100</t:betrag>', '<t:betrag>0</t:betrag>')
                .replace('<t:betrag>250.75</t:betrag>', '<t:betrag>-1</t:betrag>')

        when:
        def found = schematron(service.check(xml, Mini.bundle()))

        then:
        found*.xpath() == ['/t:rechnung/t:position[1]/t:betrag', '/t:rechnung/t:position[2]/t:betrag']
        found*.line() == [Mini.lineOf(xml, '<t:betrag>0<'), Mini.lineOf(xml, '<t:betrag>-1<')]
    }

    def "die Rolle der Regel bestimmt den Schweregrad: Warnung gilt nicht als Fehler"() {
        given:
        def xml = twoPositions.replace('<t:rabatt>10</t:rabatt>', '<t:rabatt>60</t:rabatt>')

        when:
        def result = service.check(xml, Mini.bundle())
        def found = schematron(result)
        def finding = found[0]

        then: 'successful-report wird wie failed-assert als Befund erfasst'
        found.size() == 1
        finding.ruleId() == 'TEST-0002'
        finding.severity() == Severity.WARNING
        finding.line() == Mini.lineOf(xml, '60')
        result.valid
    }

    def "ein Hinweis hat den Schweregrad Info"() {
        given:
        def extra = '<t:position art="ware"><t:betrag>1</t:betrag></t:position>' * 2
        def xml = twoPositions.replace('</t:rechnung>', extra + '</t:rechnung>')

        when:
        def result = service.check(xml, Mini.bundle())
        def found = schematron(result)
        def finding = found[0]

        then:
        found.size() == 1
        finding.ruleId() == 'TEST-0003'
        finding.severity() == Severity.INFO
        finding.line() == Mini.lineOf(xml, '<t:rechnung')
        result.valid
    }

    def "ohne Regeln im Schema läuft nur die XSD-Prüfung"() {
        given:
        def xml = twoPositions.replace('<t:betrag>250.75</t:betrag>', '<t:betrag>-5</t:betrag>')

        expect:
        schematron(service.check(xml, Mini.bundle(false))).isEmpty()
    }

    def "eine nicht ausführbare Regeldatei wird als Systemwarnung gemeldet, die Prüfung bricht nicht ab"() {
        given:
        def broken = new SchemaBundle('Mini 1.0', Mini.xsd(), [tempDir.resolve('fehlt.xsl').toUri().toURL()])

        when:
        def result = service.check(Mini.message('rechnung-einfach.xml'), broken)

        then:
        result.valid
        def finding = result.findings().find { it.origin() == Origin.SYSTEM }
        finding.severity() == Severity.WARNING
        finding.message().contains('fehlt.xsl')
        finding.message().contains('konnte nicht ausgeführt werden')
    }

    def "eine geänderte Regeldatei wirkt ohne Neustart, weil die Änderungszeit geprüft wird"() {
        given:
        def xsl = tempDir.resolve('regeln.xsl')
        Files.copy(Path.of(Mini.rules().toURI()), xsl)
        def bundle = new SchemaBundle('Mini 1.0', Mini.xsd(), [xsl.toUri().toURL()])
        def xml = twoPositions.replace('<t:betrag>250.75</t:betrag>', '<t:betrag>-5</t:betrag>')
        assert !service.check(xml, bundle).valid

        when:
        Files.writeString(xsl, Files.readString(xsl).replace('le 0', 'lt -1000'))
        Files.setLastModifiedTime(xsl, FileTime.fromMillis(System.currentTimeMillis() + 5000))

        then:
        service.check(xml, bundle).valid
    }
}
