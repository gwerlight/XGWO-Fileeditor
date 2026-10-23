package de.gwerlight.xgwo.fileedit.desktop.ui.component

import spock.lang.Specification

class IconsSpec extends Specification {

    def "das Programm-Icon liegt in drei Größen vor"() {
        expect:
        Icons.appIcons()*.getWidth(null) == [32, 64, 256]
    }

    def "die Symbolleisten-Icons sind ladbar"() {
        expect:
        ['open', 'save', 'save-as', 'check', 'specifications'].every {
            Icons.of(it).iconWidth == 19 && Icons.green(it).iconHeight == 19
        }
    }
}
