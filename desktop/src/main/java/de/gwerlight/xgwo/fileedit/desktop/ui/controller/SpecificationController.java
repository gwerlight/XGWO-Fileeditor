package de.gwerlight.xgwo.fileedit.desktop.ui.controller;

import de.gwerlight.xgwo.fileedit.core.service.XmlCheckService;
import de.gwerlight.xgwo.fileedit.desktop.config.SchemaConfiguration;
import de.gwerlight.xgwo.fileedit.desktop.config.SchemaOption;
import de.gwerlight.xgwo.fileedit.desktop.spec.Specification;
import de.gwerlight.xgwo.fileedit.desktop.spec.SpecificationStore;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Verwaltet die eingerichteten Spezifikationen: lädt sie beim Start, lässt sie über einen Dialog ändern, speichert sie
 * und stellt den daraus aufgebauten Prüfdienst bereit. Die Schema-Auswahl der Oberfläche wird bei jeder Änderung neu gesetzt.
 */
public final class SpecificationController {

    private final SpecificationStore store;
    private final Dialogs dialogs;
    private final Consumer<List<SchemaOption>> options;
    private final Consumer<String> status;
    private List<Specification> specifications = List.of();
    private SchemaConfiguration current;

    /**
     * @param options setzt die Einträge der Schema-Auswahl
     * @param status  zeigt einen Hinweis in der Statuszeile
     */
    public SpecificationController(SpecificationStore store, Dialogs dialogs, Consumer<List<SchemaOption>> options,
                    Consumer<String> status) {
        this.store = store;
        this.dialogs = dialogs;
        this.options = options;
        this.status = status;
        String loadProblem = null;
        try {
            specifications = store.load();
        } catch (UncheckedIOException e) {
            loadProblem = e.getMessage();
        }
        apply(loadProblem);
    }

    /** Aktueller Prüfdienst; ändert sich, wenn Spezifikationen eingerichtet werden. */
    public XmlCheckService service() {
        return current.service();
    }

    public List<Specification> specifications() {
        return specifications;
    }

    /** Öffnet den Dialog zum Einrichten; bei Bestätigung wird gespeichert und der Prüfdienst neu aufgebaut. */
    public void edit() {
        dialogs.editSpecifications(specifications).ifPresent(edited -> {
            try {
                store.save(edited);
            } catch (UncheckedIOException e) {
                dialogs.showError("Spezifikationen nicht speicherbar", e.getMessage());
                return;
            }
            specifications = List.copyOf(edited);
            apply(null);
        });
    }

    private void apply(String loadProblem) {
        current = SchemaConfiguration.from(specifications);
        options.accept(current.options());
        if (loadProblem != null) {
            status.accept(loadProblem);
        } else if (specifications.isEmpty()) {
            status.accept("Keine Spezifikation eingerichtet – bitte im Menü neben „Öffnen…“ unter „Spezifikationen…“ einrichten.");
        } else if (!current.problems().isEmpty()) {
            int more = current.problems().size() - 1;
            status.accept("Spezifikation nicht nutzbar: " + current.problems().get(0)
                    + (more > 0 ? " (und " + more + " weitere)" : ""));
        } else {
            status.accept(" ");
        }
    }
}
