package de.gwerlight.xgwo.fileedit.core.check

import de.gwerlight.xgwo.fileedit.core.LocalSpecDir
import de.gwerlight.xgwo.fileedit.core.model.Origin
import de.gwerlight.xgwo.fileedit.core.schema.NamespaceSchemaCatalog
import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle
import de.gwerlight.xgwo.fileedit.core.service.DefaultXmlCheckService

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Optionale Tests gegen die echte Spezifikation XGewerbeordnung 1.6. Sie laufen nur, wenn eine lokale Kopie vorliegt
 * (siehe {@link LocalSpecDir}); im Repository und in der CI fehlt sie und die Tests werden übersprungen.
 */
@Requires({ LocalSpecDir.dir() != null })
class LocalXgwoSpec extends Specification {

    static final String XGA_NS = 'http://www.xgewerbeordnung.de/spezifikation/xga/1.6'

    @Shared
    Path spec = LocalSpecDir.dir()?.resolve('XGWO_1_6')

    @Shared
    SchemaBundle bundle = spec == null ? null : new SchemaBundle('XGWO_1_6',
            spec.resolve('xsd/xgewerbeordnung-gewerbeanzeige.xsd').toUri().toURL(),
            [spec.resolve('xsl/spezifikation.xsl').toUri().toURL()])

    @Shared
    def service = DefaultXmlCheckService.withDefaults(new NamespaceSchemaCatalog([(XGA_NS): bundle]))

    static List<String> validFiles() {
        Files.list(LocalSpecDir.dir().resolve('XGWO_1_6/messages/valid')).withCloseable { s ->
            s.map { it.fileName.toString() }.filter { it.endsWith('.xml') }.sorted().collect(Collectors.toList())
        }
    }

    private String message(String file) {
        Files.readString(spec.resolve("messages/valid/${file}"))
    }

    @Unroll
    def "gültige Nachricht #file besteht XSD- und fachliche Prüfung"() {
        when:
        def result = service.check(message(file))

        then:
        result.valid
        result.schemaId() == 'XGWO_1_6'
        result.findings().isEmpty()

        where:
        file << validFiles()
    }

    def "unbekanntes Attribut am Wurzelelement wird gemeldet"() {
        given:
        def xml = message('01-ek-anmeldung-0290.xml')
                .replace('standard="XGewerbeordnung"', 'standard="XGewerbeordnung" unbekannt="x"')

        when:
        def result = service.check(xml)

        then:
        !result.valid
        result.findings().any { it.origin() == Origin.XSD && it.ruleId() == 'cvc-complex-type.3.2.2' }
        result.findings().every { it.hasPosition() }
    }

    def "Regelverstoß wird mit Regel-ID, Meldung und Position gemeldet"() {
        given: 'SCH-0001: Bei einer Anmeldung ist die Angabe neuAusgeuebt unzulässig'
        def xml = message('01-ek-anmeldung-0290.xml')
                .replace('</xga:stichwort>', '</xga:stichwort><xga:neuAusgeuebt>true</xga:neuAusgeuebt>')
        def schwerpunktLine = xml.readLines().findIndexOf { it.contains('<xga:schwerpunkt>') } + 1

        when:
        def result = service.check(xml)
        def finding = result.findings().find { it.origin() == Origin.SCHEMATRON && it.ruleId() == 'SCH-0001' }

        then:
        !result.valid
        finding != null
        finding.message().startsWith('[SCH-0001]')
        finding.line() == schwerpunktLine
        finding.xpath()
    }
}
