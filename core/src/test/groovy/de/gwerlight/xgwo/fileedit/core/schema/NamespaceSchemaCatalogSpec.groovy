package de.gwerlight.xgwo.fileedit.core.schema

import de.gwerlight.xgwo.fileedit.core.Mini
import de.gwerlight.xgwo.fileedit.core.model.Origin
import de.gwerlight.xgwo.fileedit.core.service.DefaultXmlCheckService

import spock.lang.Shared
import spock.lang.Specification

class NamespaceSchemaCatalogSpec extends Specification {

    @Shared
    SchemaBundle bundle = Mini.bundle()

    def catalog = new NamespaceSchemaCatalog([(Mini.NS): bundle])

    def "findet das Schema über den Namespace des Wurzelelements einer Nachricht"() {
        given:
        def xml = Mini.message('rechnung-einfach.xml')

        when:
        def info = XmlDocumentInfo.detect(xml)
        def found = catalog.find(info)

        then:
        info.rootLocalName() == 'rechnung'
        info.namespaceUri() == Mini.NS
        found.present
        found.get().is(bundle)
    }

    def "der Name des Wurzelelements spielt keine Rolle"() {
        expect:
        catalog.find(new XmlDocumentInfo('irgendwas', Mini.NS)).present
    }

    def "unbekannter Namespace liefert leeres Optional"() {
        expect:
        !catalog.find(new XmlDocumentInfo('rechnung', 'urn:unbekannt')).present
    }

    def "Dokument ohne Namespace liefert leeres Optional"() {
        expect:
        !catalog.find(XmlDocumentInfo.detect('<order/>')).present
    }

    def "andere Version des Namespace wird nicht zugeordnet"() {
        expect:
        !catalog.find(new XmlDocumentInfo('rechnung', Mini.NS.replace(':1', ':2'))).present
    }

    def "XmlDocumentInfo meldet ein Dokument ohne Wurzelelement als Fehler"() {
        when:
        XmlDocumentInfo.detect('   ')

        then:
        thrown(javax.xml.stream.XMLStreamException)
    }

    def "DefaultXmlCheckService meldet fehlendes Schema bei unbekanntem Namespace"() {
        given:
        def service = DefaultXmlCheckService.withDefaults(catalog)

        when:
        def result = service.check('<a xmlns="urn:unbekannt"/>')

        then:
        result.schemaId() == null
        !result.valid
        result.findings()*.origin() == [Origin.SYSTEM]
        result.findings()[0].message().contains("Wurzelelement 'a'")
    }

    def "DefaultXmlCheckService meldet ein Dokument ohne lesbares Wurzelelement als nicht wohlgeformt"() {
        when:
        def result = DefaultXmlCheckService.withDefaults(catalog).check('kein XML')

        then:
        result.schemaId() == null
        result.findings()*.origin() == [Origin.WELL_FORMED]
    }

    def "DefaultXmlCheckService prüft eine gültige Nachricht mit automatisch erkanntem Schema"() {
        given:
        def service = DefaultXmlCheckService.withDefaults(catalog)

        when:
        def result = service.check(Mini.message('rechnung-einfach.xml'))

        then:
        result.schemaId() == 'Mini 1.0'
        result.valid
    }
}
