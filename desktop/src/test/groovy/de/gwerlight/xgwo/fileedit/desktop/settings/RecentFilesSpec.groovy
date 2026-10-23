package de.gwerlight.xgwo.fileedit.desktop.settings

import java.nio.file.Path
import java.util.prefs.Preferences

import spock.lang.Specification

class RecentFilesSpec extends Specification {

    // Eigener Knoten, damit die echten Benutzereinstellungen unberührt bleiben
    Preferences prefs = Preferences.userRoot().node('de/gwerlight/xgwo/fileedit/desktop-test-' + UUID.randomUUID())

    def cleanup() {
        prefs.removeNode()
    }

    private static Path abs(String name) {
        Path.of(name).toAbsolutePath().normalize()
    }

    def "ist anfangs leer"() {
        expect:
        new RecentFiles(3, prefs).list().isEmpty()
    }

    def "neueste Datei steht vorn"() {
        given:
        def recent = new RecentFiles(3, prefs)

        when:
        recent.add(Path.of('a.xml'))
        recent.add(Path.of('b.xml'))

        then:
        recent.list() == [abs('b.xml'), abs('a.xml')]
    }

    def "erneutes Hinzufügen stellt die Datei nach vorn, ohne sie zu verdoppeln"() {
        given:
        def recent = new RecentFiles(3, prefs)
        recent.add(Path.of('a.xml'))
        recent.add(Path.of('b.xml'))

        when:
        recent.add(Path.of('a.xml'))

        then:
        recent.list() == [abs('a.xml'), abs('b.xml')]
    }

    def "ältere Einträge über dem Limit fallen heraus"() {
        given:
        def recent = new RecentFiles(3, prefs)

        when:
        ['a', 'b', 'c', 'd'].each { recent.add(Path.of(it + '.xml')) }

        then:
        recent.list() == [abs('d.xml'), abs('c.xml'), abs('b.xml')]
    }

    def "relative Pfade werden normalisiert"() {
        given:
        def recent = new RecentFiles(3, prefs)

        when:
        recent.add(Path.of('x', '..', 'a.xml'))

        then:
        recent.list() == [abs('a.xml')]
    }

    def "entfernt eine Datei"() {
        given:
        def recent = new RecentFiles(3, prefs)
        recent.add(Path.of('a.xml'))
        recent.add(Path.of('b.xml'))

        when:
        recent.remove(abs('a.xml'))

        then:
        recent.list() == [abs('b.xml')]
    }

    def "Entfernen einer unbekannten Datei ändert nichts"() {
        given:
        def recent = new RecentFiles(3, prefs)
        recent.add(Path.of('a.xml'))

        when:
        recent.remove(abs('unbekannt.xml'))

        then:
        recent.list() == [abs('a.xml')]
    }

    def "die Liste bleibt über einen Neustart erhalten"() {
        given:
        def first = new RecentFiles(3, prefs)
        first.add(Path.of('a.xml'))
        first.add(Path.of('b.xml'))

        expect:
        new RecentFiles(3, prefs).list() == [abs('b.xml'), abs('a.xml')]
    }

    def "Entfernen wird ebenfalls gespeichert"() {
        given:
        def first = new RecentFiles(3, prefs)
        first.add(Path.of('a.xml'))
        first.remove(abs('a.xml'))

        expect:
        new RecentFiles(3, prefs).list().isEmpty()
    }

    def "die zurückgegebene Liste lässt sich nicht verändern"() {
        when:
        new RecentFiles(3, prefs).list().add(Path.of('a.xml'))

        then:
        thrown(UnsupportedOperationException)
    }
}
