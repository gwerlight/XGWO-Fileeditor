package de.gwerlight.xgwo.fileedit.desktop.ui.controller

import de.gwerlight.xgwo.fileedit.desktop.config.SchemaOption
import de.gwerlight.xgwo.fileedit.desktop.spec.SpecFixtures
import de.gwerlight.xgwo.fileedit.desktop.spec.Specification
import de.gwerlight.xgwo.fileedit.desktop.spec.SpecificationStore

import java.nio.file.Files
import java.nio.file.Path

import spock.lang.Specification as SpockSpec
import spock.lang.TempDir

class SpecificationControllerSpec extends SpockSpec {

    @TempDir
    Path dir

    Dialogs dialogs = Mock()
    SpecificationStore store
    List<List<SchemaOption>> optionUpdates = []
    List<String> status = []

    def setup() {
        store = new SpecificationStore(dir.resolve('specs.properties'))
    }

    private SpecificationController controller() {
        new SpecificationController(store, dialogs, { optionUpdates << it }, { status << it })
    }

    def "ohne eingerichtete Spezifikation weist die Statuszeile auf das Einrichten hin"() {
        when:
        controller()

        then:
        optionUpdates.last()*.label() == ['Automatisch erkennen']
        status.last().contains('Spezifikationen…')
    }

    def "gespeicherte Spezifikationen stehen beim Start in der Auswahl"() {
        given:
        store.save([SpecFixtures.mini()])

        when:
        def controller = controller()

        then:
        optionUpdates.last()*.label() == ['Automatisch erkennen', 'Mini 1.0']
        status.last() == ' '
        controller.service().check(SpecFixtures.valid()).valid
    }

    def "eine gespeicherte, aber nicht mehr vorhandene Datei wird in der Statuszeile genannt"() {
        given:
        store.save([new Specification('Alt', '1', Path.of('verschwunden.xsd'), SpecFixtures.xsl())])

        when:
        controller()

        then:
        optionUpdates.last()*.label() == ['Automatisch erkennen']
        status.last().startsWith('Spezifikation nicht nutzbar: Alt 1')
    }

    def "eine unlesbare Konfigurationsdatei wird gemeldet, die Anwendung startet trotzdem"() {
        given: 'die Konfigurationsdatei ist ein Ordner'
        Files.createDirectory(dir.resolve('specs.properties'))
        Files.createFile(dir.resolve('specs.properties').resolve('x'))

        when:
        def controller = controller()

        then:
        optionUpdates.last()*.label() == ['Automatisch erkennen']
        controller.specifications().isEmpty()
    }

    def "bestätigter Dialog speichert und aktualisiert Auswahl und Prüfdienst"() {
        given:
        def controller = controller()

        when:
        controller.edit()

        then:
        1 * dialogs.editSpecifications([]) >> Optional.of([SpecFixtures.mini()])
        store.load() == [SpecFixtures.mini()]
        controller.specifications() == [SpecFixtures.mini()]
        optionUpdates.last()*.label() == ['Automatisch erkennen', 'Mini 1.0']
        status.last() == ' '
        controller.service().check(SpecFixtures.negativeAmount()).errorCount() == 1
    }

    def "abgebrochener Dialog ändert nichts"() {
        given:
        store.save([SpecFixtures.mini()])
        def controller = controller()
        int updates = optionUpdates.size()

        when:
        controller.edit()

        then:
        1 * dialogs.editSpecifications([SpecFixtures.mini()]) >> Optional.empty()
        optionUpdates.size() == updates
        store.load() == [SpecFixtures.mini()]
    }

    def "Entfernen der letzten Spezifikation geht und weist wieder auf das Einrichten hin"() {
        given:
        store.save([SpecFixtures.mini()])
        def controller = controller()

        when:
        controller.edit()

        then:
        1 * dialogs.editSpecifications(_) >> Optional.of([])
        store.load().isEmpty()
        optionUpdates.last()*.label() == ['Automatisch erkennen']
        status.last().contains('Spezifikationen…')
    }

    def "scheitert das Speichern, bleibt der bisherige Stand und der Fehler wird gezeigt"() {
        given:
        def blocked = new SpecificationStore(dir.resolve('datei'))
        Files.createDirectory(dir.resolve('datei'))
        def controller = new SpecificationController(blocked, dialogs, { optionUpdates << it }, { status << it })
        int updates = optionUpdates.size()

        when:
        controller.edit()

        then:
        1 * dialogs.editSpecifications(_) >> Optional.of([SpecFixtures.mini()])
        1 * dialogs.showError('Spezifikationen nicht speicherbar', _)
        controller.specifications().isEmpty()
        optionUpdates.size() == updates
    }
}
