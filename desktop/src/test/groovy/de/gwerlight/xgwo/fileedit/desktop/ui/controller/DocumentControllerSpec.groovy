package de.gwerlight.xgwo.fileedit.desktop.ui.controller

import de.gwerlight.xgwo.fileedit.desktop.settings.RecentFiles
import de.gwerlight.xgwo.fileedit.desktop.ui.tabs.EditorTabs

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.prefs.Preferences

import javax.swing.JMenuItem

import spock.lang.Specification
import spock.lang.TempDir

/** Prüft Öffnen, Speichern und Schließen; die Dialoge sind durch einen Mock ersetzt, die Tests laufen auch headless. */
class DocumentControllerSpec extends Specification {

    @TempDir
    Path dir

    Preferences prefs = Preferences.userRoot().node('de/gwerlight/xgwo/fileedit/desktop-test-' + UUID.randomUUID())
    RecentFiles recent = new RecentFiles(3, prefs)
    EditorTabs tabs = new EditorTabs()
    Dialogs dialogs = Mock()
    List<String> status = []
    DocumentController controller

    def setup() {
        tabs.addTab()
        controller = new DocumentController(tabs, recent, dialogs, { status << it })
    }

    def cleanup() {
        prefs.removeNode()
    }

    private Path file(String name, String text) {
        Files.writeString(dir.resolve(name), text, StandardCharsets.UTF_8)
        dir.resolve(name)
    }

    private void clickMenu(String label) {
        controller.buildFileMenu().components.find { it instanceof JMenuItem && it.text == label }.doClick()
    }

    def "die erste geöffnete Datei ersetzt den unberührten Starttab"() {
        given:
        def a = file('a.xml', '<a/>')

        when:
        controller.openAll([a])

        then:
        tabs.tabCount == 1
        tabs.active().file() == a
        tabs.active().editor().getText() == '<a/>'
        !tabs.active().isDirty()
    }

    def "weitere Dateien öffnen jeweils einen eigenen Tab"() {
        when:
        controller.openAll([file('a.xml', '<a/>'), file('b.xml', '<b/>')])

        then:
        tabs.tabCount == 2
        tabs.all()*.title() == ['a.xml', 'b.xml']
        tabs.active().title() == 'b.xml'
    }

    def "eine bereits offene Datei wird nur nach vorn geholt"() {
        given:
        def a = file('a.xml', '<a/>')
        controller.openAll([a, file('b.xml', '<b/>')])

        when:
        controller.openAll([a])

        then:
        tabs.tabCount == 2
        tabs.active().file() == a
    }

    def "ein Tab mit Änderungen wird nicht wiederverwendet"() {
        given:
        tabs.active().editor().setText('Entwurf')

        when:
        controller.openAll([file('a.xml', '<a/>')])

        then:
        tabs.tabCount == 2
        tabs.all()[0].editor().getText() == 'Entwurf'
    }

    def "UTF-8 mit Umlauten wird korrekt gelesen"() {
        when:
        controller.openAll([file('u.xml', '<a>Müller – Straße</a>')])

        then:
        tabs.active().editor().getText() == '<a>Müller – Straße</a>'
    }

    def "geöffnete Dateien landen in der Liste zuletzt geöffneter Dateien"() {
        given:
        def a = file('a.xml', '<a/>')

        when:
        controller.openAll([a])

        then:
        recent.list() == [a]
    }

    def "Dateimenü zeigt Speichern-Einträge und die zuletzt geöffneten Dateien"() {
        given:
        controller.openAll([file('a.xml', '<a/>')])

        when:
        def labels = controller.buildFileMenu().components.findAll { it instanceof JMenuItem }*.text

        then:
        labels[0] == 'Speichern'
        labels[1] == 'Speichern unter…'
        labels[2] == 'Zuletzt geöffnet'
        labels[3].startsWith('a.xml')
    }

    def "Dateimenü ohne zuletzt geöffnete Dateien zeigt (keine)"() {
        expect:
        controller.buildFileMenu().components.findAll { it instanceof JMenuItem }*.text[-1] == '(keine)'
    }

    def "Schließen ohne Änderungen entfernt den Tab ohne Rückfrage, ein leerer Tab bleibt"() {
        given:
        controller.openAll([file('a.xml', '<a/>')])

        when:
        controller.close(tabs.active())

        then:
        tabs.tabCount == 1
        tabs.active().isUntouched()
    }

    def "ohne Änderungen darf die Anwendung ohne Rückfrage enden"() {
        given:
        controller.openAll([file('a.xml', '<a/>'), file('b.xml', '<b/>')])

        expect:
        controller.confirmDiscardAll()
    }

    // ---- Öffnen mit Dialog ----

    def "Öffnen-Dialog öffnet alle gewählten Dateien"() {
        given:
        def a = file('a.xml', '<a/>')
        def b = file('b.xml', '<b/>')

        when:
        controller.openWithDialog()

        then:
        1 * dialogs.chooseFilesToOpen(null) >> [a, b]
        tabs.all()*.title() == ['a.xml', 'b.xml']
    }

    def "Abbruch im Öffnen-Dialog ändert nichts"() {
        when:
        controller.openWithDialog()

        then:
        1 * dialogs.chooseFilesToOpen(null) >> []
        tabs.tabCount == 1
        tabs.active().isUntouched()
    }

    def "der Öffnen-Dialog startet im Ordner der aktiven Datei"() {
        given:
        controller.openAll([file('a.xml', '<a/>')])

        when:
        controller.openWithDialog()

        then:
        1 * dialogs.chooseFilesToOpen(dir) >> []
    }

    def "eine nicht lesbare Datei meldet den Fehler und verschwindet aus den zuletzt geöffneten"() {
        given:
        def missing = dir.resolve('fehlt.xml')
        recent.add(missing)

        when:
        controller.openAll([missing])

        then:
        1 * dialogs.showError('Datei nicht lesbar', _)
        recent.list().isEmpty()
        tabs.tabCount == 1
        tabs.active().isUntouched()
    }

    // ---- Speichern ----

    def "Speichern schreibt die Datei des aktiven Tabs ohne Dialog"() {
        given:
        def a = file('a.xml', '<a/>')
        controller.openAll([a])
        tabs.active().editor().setText('<geändert/>')

        when:
        clickMenu('Speichern')

        then:
        0 * dialogs._
        Files.readString(a) == '<geändert/>'
        !tabs.active().isDirty()
        status == ['Gespeichert: a.xml']
    }

    def "Speichern eines unbenannten Dokuments fragt nach dem Ziel"() {
        given:
        def target = dir.resolve('neu.xml')
        tabs.active().editor().setText('<neu/>')

        when:
        clickMenu('Speichern')

        then:
        1 * dialogs.chooseSaveTarget(null, null) >> Optional.of(target)
        Files.readString(target) == '<neu/>'
        tabs.active().file() == target
        !tabs.active().isDirty()
        recent.list() == [target]
    }

    def "Abbruch bei Speichern unter schreibt nichts"() {
        given:
        tabs.active().editor().setText('<neu/>')

        when:
        clickMenu('Speichern unter…')

        then:
        1 * dialogs.chooseSaveTarget(*_) >> Optional.empty()
        tabs.active().isDirty()
        tabs.active().file() == null
        Files.list(dir).count() == 0
    }

    def "Speichern unter schreibt in die neue Datei und lässt die alte unverändert"() {
        given:
        def a = file('a.xml', '<a/>')
        def b = dir.resolve('b.xml')
        controller.openAll([a])
        tabs.active().editor().setText('<neu/>')

        when:
        clickMenu('Speichern unter…')

        then:
        1 * dialogs.chooseSaveTarget(dir, a) >> Optional.of(b)
        Files.readString(a) == '<a/>'
        Files.readString(b) == '<neu/>'
        tabs.active().file() == b
    }

    def "eine vorhandene fremde Datei wird nur nach Bestätigung überschrieben"() {
        given:
        def other = file('other.xml', 'alt')
        tabs.active().editor().setText('<neu/>')

        when:
        clickMenu('Speichern unter…')

        then:
        1 * dialogs.chooseSaveTarget(*_) >> Optional.of(other)
        1 * dialogs.confirmOverwrite(other) >> answer
        Files.readString(other) == content

        where:
        answer | content
        false  | 'alt'
        true   | '<neu/>'
    }

    def "Speichern unter in die eigene Datei fragt nicht nach dem Überschreiben"() {
        given:
        def a = file('a.xml', '<a/>')
        controller.openAll([a])
        tabs.active().editor().setText('<neu/>')

        when:
        clickMenu('Speichern unter…')

        then:
        1 * dialogs.chooseSaveTarget(*_) >> Optional.of(a)
        0 * dialogs.confirmOverwrite(_)
        Files.readString(a) == '<neu/>'
    }

    def "ein Schreibfehler wird gemeldet und das Dokument bleibt geändert"() {
        given:
        tabs.active().editor().setText('<neu/>')

        when:
        clickMenu('Speichern unter…')

        then:
        1 * dialogs.chooseSaveTarget(*_) >> Optional.of(dir.resolve('gibt-es-nicht').resolve('x.xml'))
        1 * dialogs.showError('Datei nicht speicherbar', _)
        tabs.active().isDirty()
        tabs.active().file() == null
        status.isEmpty()
    }

    // ---- Schließen mit Rückfrage ----

    def "Schließen mit Änderungen: Speichern sichert und schließt"() {
        given:
        def a = file('a.xml', '<a/>')
        controller.openAll([a])
        tabs.active().editor().setText('<neu/>')

        when:
        controller.close(tabs.active())

        then:
        1 * dialogs.askSaveChanges('a.xml') >> Dialogs.SaveChoice.SAVE
        Files.readString(a) == '<neu/>'
        tabs.active().isUntouched()
    }

    def "Schließen mit Änderungen: Nicht speichern verwirft die Änderungen"() {
        given:
        def a = file('a.xml', '<a/>')
        controller.openAll([a])
        tabs.active().editor().setText('<neu/>')

        when:
        controller.close(tabs.active())

        then:
        1 * dialogs.askSaveChanges('a.xml') >> Dialogs.SaveChoice.DISCARD
        Files.readString(a) == '<a/>'
        tabs.active().isUntouched()
    }

    def "Schließen mit Änderungen: Abbrechen lässt den Tab offen"() {
        given:
        controller.openAll([file('a.xml', '<a/>')])
        tabs.active().editor().setText('<neu/>')

        when:
        controller.close(tabs.active())

        then:
        1 * dialogs.askSaveChanges('a.xml') >> Dialogs.SaveChoice.CANCEL
        tabs.active().file() != null
        tabs.active().isDirty()
    }

    def "Speichern in der Rückfrage mit abgebrochenem Zieldialog lässt den Tab offen"() {
        given:
        tabs.active().editor().setText('<neu/>')

        when:
        controller.close(tabs.active())

        then:
        1 * dialogs.askSaveChanges('Unbenannt') >> Dialogs.SaveChoice.SAVE
        1 * dialogs.chooseSaveTarget(*_) >> Optional.empty()
        tabs.active().isDirty()
    }

    def "beim Beenden wird für jeden geänderten Tab gefragt, bei Abbruch endet die Anwendung nicht"() {
        given:
        controller.openAll([file('a.xml', '<a/>'), file('b.xml', '<b/>')])
        tabs.all().each { it.editor().append(' ') }

        when:
        def allowed = controller.confirmDiscardAll()

        then:
        1 * dialogs.askSaveChanges('a.xml') >> Dialogs.SaveChoice.DISCARD
        1 * dialogs.askSaveChanges('b.xml') >> Dialogs.SaveChoice.CANCEL
        !allowed
    }

    def "beim Beenden dürfen alle Änderungen verworfen werden"() {
        given:
        controller.openAll([file('a.xml', '<a/>'), file('b.xml', '<b/>')])
        tabs.all().each { it.editor().append(' ') }

        when:
        def allowed = controller.confirmDiscardAll()

        then:
        2 * dialogs.askSaveChanges(_) >> Dialogs.SaveChoice.DISCARD
        allowed
    }
}