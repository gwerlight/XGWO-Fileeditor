# XGWO-FileEditor

Diagnose-Werkzeug zum Prüfen und Korrigieren von XML-Meldungen nach dem Standard
[XGewerbeordnung](https://xgewerbeordnung.de/). Der Editor öffnet eine XML-Datei, prüft sie gegen XSD und Schematron
und zeigt jeden Befund mit Regel, Meldung und Zeile. Ein Klick springt zur Stelle im Text; die Datei lässt sich direkt
korrigieren, erneut prüfen und speichern.

Das Programm ist kein offizielles Werkzeug des Herausgebers. Die **„Spezifikation XGewerbeordnung“**
([XRepository](https://www.xrepository.de/details/urn:xoev-de:kosit:standard:xgewerbeordnung_1.6#version)) ist
**nicht Teil des Programms**: Du richtest sie einmal selbst ein (siehe unten) und legst bei einer neuen Version einfach
die neue Version zusätzlich an.

## Was der Editor kann
- **Mehrere Dateien in Tabs**, auch per Kommandozeile oder „Öffnen mit“ übergeben; ungespeicherte Änderungen werden
  beim Schließen erfragt.
- **Prüfen gegen XSD und Schematron** mit Befundliste (Schweregrad, Regel-ID, Meldung, Zeile) und Sprung zur Zeile.
- **Automatische Schema-Erkennung** über den Namespace des Wurzelelements. Alternativ lässt sich die Spezifikation in
  der Symbolleiste fest wählen.
- **Mehrere Spezifikationen und Versionen** nebeneinander, jeweils mit eigenem Namen und eigener Version.
- **Bearbeiten und Speichern** mit Syntaxhervorhebung; zuletzt geöffnete Dateien stehen im Dropdown neben „Öffnen“.

## Schnellstart
1. Programm starten (siehe [Bauen und Verteilen](#bauen-und-verteilen)).
2. Einmalig eine Spezifikation einrichten (nächster Abschnitt).
3. XML-Datei öffnen und auf **Prüfen** klicken.

## Spezifikation einrichten
Der Editor kennt von sich aus keine Spezifikation. Er braucht von jeder Spezifikation zwei Dateien: das **XML-Schema
(XSD)** und die **Schematron-Regeln als XSL**. Beide gehören zur Veröffentlichung der Spezifikation und sind Pflicht.

### 1. Spezifikation herunterladen
Die XGewerbeordnung 1.6 gibt es öffentlich im XRepository:
<https://www.xrepository.de/details/urn:xoev-de:kosit:standard:xgewerbeordnung_1.6#version>

Dort das Paket der Version herunterladen und entpacken. Es gelten die Nutzungsbedingungen des Herausgebers. Gebraucht
werden daraus nur zwei Dateien. Bei XGWO 1.6 sind das (die Namen können je nach Paket abweichen):

| Datei | Beispiel (XGWO 1.6) | Hinweis |
|-------|---------------------|---------|
| XSD (Hauptdatei) | `xgewerbeordnung-gewerbeanzeige.xsd` | Die Hauptdatei, die die anderen XSDs einbindet. Alle XSDs der Spezifikation müssen im Ordner und in den Unterordnern so liegen bleiben, wie sie im Paket sind. |
| Schematron-XSL | `spezifikation.xsl` | Die fertig übersetzte Regeldatei (XSL), nicht die `.sch`-Quelle. |

### 2. Im Editor anlegen
Symbolleiste → Button **Spezifikationen** (Schieberegler-Symbol) oder im Dropdown neben „Öffnen“ den Eintrag
**Spezifikationen…** → **Hinzufügen…**:

1. Pfad zur **XSD-Hauptdatei** und zur **Schematron-XSL** wählen.
2. **Version** mit dem Zahlenfeld einstellen (z. B. 1.6, 1.7, 2.0).
3. Sobald alles passt, erscheint der **Name** (aus der XSD) und **OK** wird frei.

Die gesetzte Version steuert die Prüfung: Sie muss im `targetNamespace` der XSD und in einem Namespace der XSL
vorkommen, zum Beispiel `…/xga/1.6` und `…/erl/1.6`. Damit fällt sofort auf, wenn XSD und XSL aus verschiedenen
Versionen stammen. Nur wenn die Prüfung besteht, wird der Name aus der XSD übernommen (`nameKurz`); mehr über den
Inhalt der Dateien wertet der Editor bewusst nicht aus, damit Änderungen an der Spezifikation ihn möglichst nicht
berühren. Beim Bestätigen wird die XSD zusätzlich vollständig geladen, fehlende Teildateien werden dabei gemeldet.

### Hinweise
- Gemerkt werden nur die **Pfade**, keine Kopien: in `%APPDATA%\XGWO-FileEditor\specifications.properties` (sonst
  `~/.config/xgwo-fileedit`). Verschiebst oder löschst du die Dateien, meldet der Editor die Spezifikation als nicht
  nutzbar, bis du den Pfad korrigierst.
- Ein **Update** der Spezifikation ist eine neue Version im Dialog, keine neue Programmversion. Die alte Version bleibt
  zum Vergleich wählbar.
- Ohne Eintrag zeigt die Statuszeile den Hinweis, eine Spezifikation einzurichten.

## Mitwirken und Fehler melden
Fehler und Wünsche gerne als [Issue](https://github.com/gwerlight/XGWO-FileEditor/issues). Bitte hänge **keine echten
Meldungen mit personenbezogenen Daten** an, sondern nur anonymisierte oder erfundene Beispiele. Die Spezifikationsdateien
selbst gehören nicht ins Repository.

## Lizenz
Der Quellcode steht unter der [Apache License 2.0](LICENSE). Das gilt nicht für die Spezifikation XGewerbeordnung; für
sie gelten die Nutzungsbedingungen des Herausgebers.

## Module
- `core`    – Fachlogik ohne UI/Spring: `XmlCheckService`, `CheckStep`, `SchemaCatalog`, `Finding`
- `desktop` – Swing-Oberfläche (FlatLaf, RSyntaxTextArea)
- `web`     – (später) Spring Boot/Vaadin, nutzt nur `core`

## Tests
Die Tests laufen mit eigenen Minimal-Dateien (`core/src/test/resources/spec`, `desktop/src/test/resources/spec`), die nur
die Grundfunktion prüfen. Tests gegen die echte Spezifikation sind optional und laufen nur, wenn eine lokale Kopie
vorliegt: Ordner `local-spec/XGWO_1_6/{xsd,xsl,messages/valid}` im Projekt (steht in der `.gitignore`) oder
`-Dxgwo.spec.dir=<Pfad>`. Ohne Kopie werden sie übersprungen.

## Bauen und Verteilen
Zwei Wege, das Programm zu nutzen:

1. **Fat-JAR, Java muss installiert sein** (Java 17 oder neuer): eine Datei, Start mit
   `java -jar xgwo-fileedit-desktop-<Version>-all.jar [Datei.xml …]`. Baut `./mvnw clean package` (Maven Wrapper, bringt Maven selbst mit; unter Windows `mvnw.cmd`)
   (Ergebnis `desktop/target/*-all.jar`).
2. **Portable Fassung mit eigener Java-Runtime** (ca. 140 MB, kein Java nötig): `./mvnw package -Papp-image` erzeugt den
   Ordner `desktop/target/dist/XGWO-FileEditor`; ZIP entpacken und `XGWO-FileEditor.exe` starten. Dateien lassen sich
   als Argument übergeben.

Die CI baut beides bei jedem Push auf `main` und legt sie als Artefakte `fat-jar` und `windows-portable` ab.
