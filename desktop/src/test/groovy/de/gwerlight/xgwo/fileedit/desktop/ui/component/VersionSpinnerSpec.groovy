package de.gwerlight.xgwo.fileedit.desktop.ui.component

import spock.lang.Specification
import spock.lang.Unroll

class VersionSpinnerSpec extends Specification {

    def spinner = new VersionSpinner()

    def "startet bei 1.6 (aktuelle Version im XRepository)"() {
        expect:
        spinner.version() == '1.6'
    }

    @Unroll
    def "Schritt nach oben von #start ergibt #next"() {
        given:
        spinner.setVersion(start)

        when:
        spinner.setValue(spinner.getNextValue())

        then:
        spinner.version() == next

        where:
        start | next
        '1.6' | '1.7'
        '1.9' | '2.0'
        '2.0' | '2.1'
        '0.9' | '1.0'
    }

    def "Schritt nach unten von 2.0 ergibt 1.9"() {
        given:
        spinner.setVersion('2.0')

        when:
        spinner.setValue(spinner.getPreviousValue())

        then:
        spinner.version() == '1.9'
    }

    def "viele Schritte summieren keinen Rundungsfehler auf"() {
        when:
        20.times { spinner.setValue(spinner.getNextValue()) }

        then:
        spinner.version() == '3.6'
    }

    def "an den Grenzen gibt es keinen weiteren Schritt"() {
        expect:
        spinner.setVersion('0.1')
        spinner.getPreviousValue() == null
        spinner.setVersion('99.9')
        spinner.getNextValue() == null
    }

    @Unroll
    def "setVersion('#text') ergibt #expected"() {
        given:
        spinner.setVersion('1.6')

        when:
        spinner.setVersion(text)

        then:
        spinner.version() == expected

        where:
        text     || expected
        '2.1'    || '2.1'
        ' 1.7 '  || '1.7'
        '3'      || '3.0'
        '1.66'   || '1.7'
        'abc'    || '1.6'
        ''       || '1.6'
        null     || '1.6'
        '0'      || '1.6'
        '100'    || '1.6'
    }

    def "die Anzeige verwendet einen Punkt"() {
        given:
        spinner.setVersion('1.6')

        expect:
        spinner.editor.textField.text == '1.6'
    }
}
