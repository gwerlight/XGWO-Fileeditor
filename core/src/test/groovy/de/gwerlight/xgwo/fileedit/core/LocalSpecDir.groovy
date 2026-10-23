package de.gwerlight.xgwo.fileedit.core

import java.nio.file.Files
import java.nio.file.Path

/**
 * Findet die lokale Kopie einer echten Spezifikation für die optionalen Tests. Sie gehört nicht zum Repository
 * ({@code local-spec/} steht in der {@code .gitignore}). Ort: {@code -Dxgwo.spec.dir=<Pfad>}, sonst {@code local-spec}
 * im Projekt. Aufbau darin: {@code XGWO_1_6/xsd}, {@code XGWO_1_6/xsl}, {@code XGWO_1_6/messages/valid}.
 */
class LocalSpecDir {

    /** @return das Verzeichnis mit den lokalen Spezifikationen oder {@code null}, wenn keines vorhanden ist */
    static Path dir() {
        def configured = System.getProperty('xgwo.spec.dir')
        def candidates = configured ? [Path.of(configured)] : [Path.of('local-spec'), Path.of('..', 'local-spec')]
        candidates.find { Files.isDirectory(it.resolve('XGWO_1_6')) }?.toAbsolutePath()?.normalize()
    }
}
