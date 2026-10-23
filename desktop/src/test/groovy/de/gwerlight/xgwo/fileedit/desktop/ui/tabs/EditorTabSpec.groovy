package de.gwerlight.xgwo.fileedit.desktop.ui.tabs

import java.nio.file.Path

import spock.lang.Specification

class EditorTabSpec extends Specification {

    int changes = 0
    EditorTab tab = new EditorTab({ changes++ })

    def "ein neuer Tab ist unberührt und heißt Unbenannt"() {
        expect:
        tab.isUntouched()
        !tab.isDirty()
        tab.file() == null
        tab.title() == 'Unbenannt'
        tab.tooltip() == 'Unbenannt'
    }

    def "Tippen macht den Tab geändert und meldet das genau einmal"() {
        when:
        tab.editor().setText('<a/>')
        tab.editor().append('<b/>')

        then:
        tab.isDirty()
        !tab.isUntouched()
        tab.title() == 'Unbenannt *'
        changes == 1
    }

    def "Laden setzt Text und Datei und gilt als gespeichert"() {
        when:
        tab.loaded(Path.of('Beispiel.xml'), '<a/>')

        then:
        tab.editor().getText() == '<a/>'
        tab.file() == Path.of('Beispiel.xml').toAbsolutePath().normalize()
        !tab.isDirty()
        !tab.isUntouched()
        tab.title() == 'Beispiel.xml'
        tab.tooltip() == tab.file().toString()
    }

    def "Änderung nach dem Laden markiert den Tab, Speichern setzt die Markierung zurück"() {
        given:
        tab.loaded(Path.of('Beispiel.xml'), '<a/>')

        when:
        tab.editor().append(' ')

        then:
        tab.isDirty()
        tab.title() == 'Beispiel.xml *'

        when:
        tab.markSaved(Path.of('Anders.xml'))

        then:
        !tab.isDirty()
        tab.title() == 'Anders.xml'
    }

    def "Prüfergebnis wird je Tab gemerkt und die Liste kopiert"() {
        given:
        def list = []

        when:
        tab.setCheckResult(list, 'Keine Fehler')

        then:
        tab.checkStatus() == 'Keine Fehler'
        tab.findings().isEmpty()
    }

    def "goToLine setzt den Cursor an den Zeilenanfang und ignoriert ungültige Zeilen"() {
        given:
        tab.loaded(Path.of('Beispiel.xml'), 'eins\nzwei\ndrei')

        when:
        tab.goToLine(2)

        then:
        tab.editor().getCaretPosition() == 5

        when:
        tab.goToLine(99)

        then:
        noExceptionThrown()
        tab.editor().getCaretPosition() == 5
    }
}
