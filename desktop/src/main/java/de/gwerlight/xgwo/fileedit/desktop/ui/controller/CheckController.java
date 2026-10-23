package de.gwerlight.xgwo.fileedit.desktop.ui.controller;

import de.gwerlight.xgwo.fileedit.core.model.CheckResult;
import de.gwerlight.xgwo.fileedit.core.service.XmlCheckService;
import de.gwerlight.xgwo.fileedit.desktop.config.SchemaOption;
import de.gwerlight.xgwo.fileedit.desktop.ui.panel.ToolbarPanel;
import de.gwerlight.xgwo.fileedit.desktop.ui.tabs.EditorTab;
import de.gwerlight.xgwo.fileedit.desktop.ui.tabs.EditorTabs;
import java.awt.Component;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/** Führt die Prüfung für den aktiven Tab im Hintergrund aus, damit das Fenster bedienbar bleibt. */
public final class CheckController {

    private final Supplier<XmlCheckService> service;
    private final EditorTabs tabs;
    private final ToolbarPanel toolbar;
    private final Component parent;
    private final Consumer<EditorTab> showResult;
    /** Die erste Prüfung lädt die (große) Schematron-Regeldatei und dauert deshalb spürbar länger. */
    private boolean firstCheckDone;

    /** @param showResult zeigt das Ergebnis eines Tabs an (Befundliste, Status); wird nur für den aktiven Tab aufgerufen */
    public CheckController(Supplier<XmlCheckService> service, EditorTabs tabs, ToolbarPanel toolbar, Component parent,
                    Consumer<EditorTab> showResult) {
        this.service = service;
        this.tabs = tabs;
        this.toolbar = toolbar;
        this.parent = parent;
        this.showResult = showResult;
    }

    public void run() {
        EditorTab tab = tabs.active();
        String xml = tab.editor().getText();
        SchemaOption option = toolbar.selectedSchema();
        XmlCheckService checker = service.get();
        setBusy(true);
        new SwingWorker<CheckResult, Void>() {
            @Override protected CheckResult doInBackground() {
                return option == null || option.bundle() == null
                        ? checker.check(xml)
                        : checker.check(xml, option.bundle());
            }

            // Der Tab-Vergleich per == ist gewollt: Es geht um genau diese Tab-Instanz, nicht um einen gleichen Inhalt
            @SuppressWarnings("PMD.CompareObjectsWithEquals")
            @Override protected void done() {
                setBusy(false);
                try {
                    CheckResult result = get();
                    tab.setCheckResult(result.findings(), summary(result));
                    firstCheckDone = true;
                    if (tabs.active() == tab) { // der Benutzer kann inzwischen zu einem anderen Tab gewechselt sein
                        showResult.accept(tab);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    toolbar.setStatus(" ");
                    JOptionPane.showMessageDialog(parent, ex.getCause().getMessage(),
                            "Prüfung nicht möglich", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        toolbar.setBusy(busy);
        if (busy) {
            toolbar.setStatus(firstCheckDone
                    ? "Prüfung läuft…"
                    : "Prüfung läuft – beim ersten Mal werden die Regeln geladen, das dauert einen Moment…");
        }
    }

    private static String summary(CheckResult result) {
        return result.isValid()
                ? "Keine Fehler" + (result.schemaId() != null ? " (Schema: " + result.schemaId() + ")" : "")
                : result.errorCount() + " Fehler";
    }
}
