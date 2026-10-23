package de.gwerlight.xgwo.fileedit.desktop.spec

import java.nio.file.Files
import java.nio.file.Path

import spock.lang.Specification as SpockSpec
import spock.lang.TempDir

class SpecificationStoreSpec extends SpockSpec {

    @TempDir
    Path dir

    def "ohne Datei sind keine Spezifikationen eingerichtet"() {
        expect:
        new SpecificationStore(dir.resolve('gibt-es-nicht.properties')).load().isEmpty()
    }

    def "gespeicherte Spezifikationen werden unverändert und in derselben Reihenfolge geladen"() {
        given:
        def store = new SpecificationStore(dir.resolve('specs.properties'))
        def specs = [
                new Specification('XGWO', '1.6', Path.of('C:\\Spezifikationen\\xgwo\\a.xsd'), Path.of('C:\\Spezifikationen\\xgwo\\a.xsl')),
                new Specification('Müller & Söhne', '', dir.resolve('b.xsd'), dir.resolve('b.xsl'))]

        when:
        store.save(specs)

        then:
        new SpecificationStore(dir.resolve('specs.properties')).load() == specs
    }

    def "Speichern legt fehlende Ordner an"() {
        given:
        def store = new SpecificationStore(dir.resolve('neu/unter/ordner/specs.properties'))

        when:
        store.save([SpecFixtures.mini()])

        then:
        Files.isRegularFile(store.file())
        store.load().size() == 1
    }

    def "Speichern ersetzt den früheren Stand"() {
        given:
        def store = new SpecificationStore(dir.resolve('specs.properties'))
        store.save([SpecFixtures.mini('1.0'), SpecFixtures.mini('2.0')])

        when:
        store.save([SpecFixtures.mini('3.0')])

        then:
        store.load()*.version() == ['3.0']
    }

    def "unvollständige Einträge werden übersprungen"() {
        given:
        def file = dir.resolve('specs.properties')
        Files.writeString(file, '''count=2
1.name=Halb
1.xsd=a.xsd
2.name=Ganz
2.xsd=b.xsd
2.xsl=b.xsl
''')

        expect:
        new SpecificationStore(file).load()*.name() == ['Ganz']
    }

    def "eine kaputte Anzahl führt zu einer leeren Liste statt zu einem Absturz"() {
        given:
        def file = dir.resolve('specs.properties')
        Files.writeString(file, 'count=viele\n')

        expect:
        new SpecificationStore(file).load().isEmpty()
    }

    def "nicht schreibbares Ziel wird als Fehler gemeldet"() {
        given: 'ein Ordner liegt dort, wo die Datei stehen soll'
        def file = dir.resolve('specs.properties')
        Files.createDirectory(file)

        when:
        new SpecificationStore(file).save([SpecFixtures.mini()])

        then:
        thrown(UncheckedIOException)
    }

    def "die Bezeichnung nennt Name und Version"() {
        expect:
        new Specification(name, version, null, null).label() == label

        where:
        name   | version || label
        'XGWO' | '1.6'   || 'XGWO 1.6'
        'XGWO' | ''      || 'XGWO'
        'XGWO' | null    || 'XGWO'
    }
}
