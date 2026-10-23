package de.gwerlight.xgwo.fileedit.desktop.spec

import java.nio.file.Files
import java.nio.file.Path

import spock.lang.Specification as SpockSpec
import spock.lang.TempDir

class SpecificationChecksSpec extends SpockSpec {

    @TempDir
    Path dir

    private Path write(String name, String text) {
        Files.writeString(dir.resolve(name), text)
        dir.resolve(name)
    }

    def "vollständige und richtige Dateien haben keine Probleme"() {
        expect:
        SpecificationChecks.problems('1.0', SpecFixtures.xsd(), SpecFixtures.xsl()).isEmpty()
    }

    def "die Version ist Pflicht und muss eine Zahl sein: '#version'"() {
        when:
        def problems = SpecificationChecks.problems(version, SpecFixtures.xsd(), SpecFixtures.xsl())

        then:
        problems == ['Die Version fehlt oder ist keine Zahl (z. B. 1.6).']

        where:
        version << [null, '', '   ', 'abc', 'v1.0', '1.', '1..0', '1.0 beta']
    }

    def "die Version muss im Namespace der XSD und in einem Namespace der XSL vorkommen"() {
        when:
        def problems = SpecificationChecks.problems('2.0', SpecFixtures.xsd(), SpecFixtures.xsl())

        then:
        problems.size() == 2
        problems[0].contains('Namespace der XSD')
        problems[0].contains(SpecFixtures.NS)
        problems[1].contains('Schematron-XSL deklariert keinen Namespace mit der Version 2.0')
    }

    def "eine XSL ohne Namespace der Version gehört nicht zur Spezifikation"() {
        given:
        def xsl = write('andere.xsl', Files.readString(SpecFixtures.xsl()).replace('test:1.0', 'test:9.9'))

        expect:
        SpecificationChecks.problems('1.0', SpecFixtures.xsd(), xsl)*.startsWith('Die Schematron-XSL deklariert') == [true]
    }

    def "Versionsnummer wird nur als eigenständige Zahl im Namespace erkannt: #version in #namespace"() {
        expect:
        SpecificationChecks.containsVersion(namespace, version) == expected

        where:
        namespace                                                    | version || expected
        'http://www.xgewerbeordnung.de/spezifikation/xga/1.6'        | '1.6'   || true
        'http://www.xgewerbeordnung.de/spezifikation/erl/1.6'        | '1.6'   || true
        'urn:xoev-de:xunternehmen:standard:basismodul:adressen_1.2'  | '1.2'   || true
        'http://xoev.de/schemata/basisnachricht/g2g/1_1'             | '1.1'   || true
        'http://www.xgewerbeordnung.de/spezifikation/xga/1.6'        | '1.7'   || false
        'http://www.xgewerbeordnung.de/spezifikation/xga/11.6'       | '1.6'   || false
        'http://www.xgewerbeordnung.de/spezifikation/xga/1.60'       | '1.6'   || false
        'http://www.xgewerbeordnung.de/spezifikation/xga/1.6.2'      | '1.6'   || false
        'http://www.xgewerbeordnung.de/spezifikation/xga/16'         | '1.6'   || false
        'urn:test:2'                                                 | '2'     || true
    }

    def "der Name steht in der XSD"() {
        expect:
        SpecificationChecks.specificationName(SpecFixtures.xsd()) == 'Mini'
    }

    def "ohne Angaben in der XSD ist der Dateiname der Name"() {
        given:
        def xsd = write('meine-spezifikation.xsd', '<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" targetNamespace="urn:x:1"/>')

        expect:
        SpecificationChecks.specificationName(xsd) == 'meine-spezifikation'
    }

    def "nur die erste Annotation der XSD zählt, nicht ein späteres nameKurz"() {
        given:
        def xsd = write('spaet.xsd', """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" targetNamespace="urn:x:1">
    <xs:element name="a" type="xs:string"/>
    <xs:annotation><xs:appinfo><standard><nameKurz>Falsch</nameKurz></standard></xs:appinfo></xs:annotation>
</xs:schema>""")

        expect:
        SpecificationChecks.specificationName(xsd) == 'spaet'
    }

    def "fehlende oder leere Pfade werden gemeldet"() {
        expect:
        SpecificationChecks.problems('1.0', xsd, xsl) == problems

        where:
        xsd                        | xsl                        || problems
        null                       | SpecFixtures.xsl()         || ['Die XSD-Datei fehlt oder existiert nicht.']
        Path.of('gibt-es-nicht')   | SpecFixtures.xsl()         || ['Die XSD-Datei fehlt oder existiert nicht.']
        SpecFixtures.xsd()         | null                       || ['Die Schematron-XSL fehlt oder existiert nicht (sie gehört zwingend zur Spezifikation).']
    }

    def "die Schematron-XSL ist Pflicht, auch wenn die XSD stimmt"() {
        when:
        def problems = SpecificationChecks.problems('1.0', SpecFixtures.xsd(), Path.of('fehlt.xsl'))

        then:
        problems.size() == 1
        problems[0].contains('zwingend')
    }

    def "vertauschte Dateien werden erkannt"() {
        expect:
        SpecificationChecks.problems('1.0', SpecFixtures.xsl(), SpecFixtures.xsd()) == [
                'Die XSD-Datei ist keine XML-Schema-Datei.',
                'Die Schematron-XSL ist kein XSLT-Stylesheet.']
    }

    def "keine XML-Datei wird erkannt"() {
        given:
        def text = write('notiz.txt', 'das ist kein XML')

        expect:
        SpecificationChecks.problems('1.0', text, text).size() == 2
    }

    def "eine XSD ohne targetNamespace wird abgelehnt, weil das Schema nicht erkannt werden könnte"() {
        given:
        def xsd = write('ohne.xsd', '<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>')

        expect:
        SpecificationChecks.problems('1.0', xsd, SpecFixtures.xsl())[0].contains('targetNamespace')
    }

    def "der targetNamespace wird aus der XSD gelesen"() {
        expect:
        SpecificationChecks.targetNamespace(SpecFixtures.xsd()) == Optional.of(SpecFixtures.NS)
        SpecificationChecks.targetNamespace(Path.of('fehlt.xsd')) == Optional.empty()
    }

    def "eine ladbare XSD hat keinen Ladefehler"() {
        expect:
        SpecificationChecks.loadError(SpecFixtures.xsd()).isEmpty()
    }

    def "eine XSD mit fehlender Include-Datei hat einen Ladefehler"() {
        given:
        def xsd = write('kaputt.xsd', """<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" targetNamespace="urn:x">
    <xs:include schemaLocation="gibt-es-nicht.xsd"/>
</xs:schema>""")

        expect:
        SpecificationChecks.loadError(xsd).isPresent()
    }
}
