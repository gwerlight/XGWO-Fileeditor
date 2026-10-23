package de.gwerlight.xgwo.fileedit.desktop.ui.controller;

import de.gwerlight.xgwo.fileedit.desktop.spec.Specification;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Die Rückfragen und Dateiauswahlen des {@link DocumentController}; austauschbar, damit die Logik ohne Dialoge testbar ist. */
public interface Dialogs {

    /** Antwort auf die Frage nach ungespeicherten Änderungen. */
    enum SaveChoice { SAVE, DISCARD, CANCEL }

    /** @param startDirectory Startordner oder {@code null}  @return gewählte Dateien, leer bei Abbruch */
    List<Path> chooseFilesToOpen(Path startDirectory);

    /** @param current bisherige Datei des Dokuments oder {@code null}  @return Zieldatei, leer bei Abbruch */
    Optional<Path> chooseSaveTarget(Path startDirectory, Path current);

    boolean confirmOverwrite(Path target);

    /** @param documentName Anzeigename des Dokuments (ohne Änderungsmarkierung) */
    SaveChoice askSaveChanges(String documentName);

    /** @param current die bisher eingerichteten Spezifikationen  @return die geänderte Liste, leer bei Abbruch */
    Optional<List<Specification>> editSpecifications(List<Specification> current);

    void showError(String title, String message);
}
