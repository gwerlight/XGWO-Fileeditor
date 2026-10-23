package de.gwerlight.xgwo.fileedit.desktop.ui;

import de.gwerlight.xgwo.fileedit.desktop.settings.RecentFiles;
import de.gwerlight.xgwo.fileedit.desktop.spec.SpecificationStore;
import de.gwerlight.xgwo.fileedit.desktop.ui.component.Icons;
import de.gwerlight.xgwo.fileedit.desktop.ui.controller.CheckController;
import de.gwerlight.xgwo.fileedit.desktop.ui.controller.DocumentController;
import de.gwerlight.xgwo.fileedit.desktop.ui.controller.SpecificationController;
import de.gwerlight.xgwo.fileedit.desktop.ui.controller.SwingDialogs;
import de.gwerlight.xgwo.fileedit.desktop.ui.panel.FindingsPanel;
import de.gwerlight.xgwo.fileedit.desktop.ui.panel.ToolbarPanel;
import de.gwerlight.xgwo.fileedit.desktop.ui.tabs.EditorTab;
import de.gwerlight.xgwo.fileedit.desktop.ui.tabs.EditorTabs;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

/**
 * Hauptfenster: Symbolleiste oben, Dokumente in Tabs, darunter die Befundliste des aktiven Tabs. Das Fenster
 * verdrahtet nur die Teile; die Logik liegt in {@link DocumentController} (Dateien) und {@link CheckController} (Prüfung).
 */
public final class MainFrame extends JFrame {

    private static final String APP_TITLE = "XGWO-FileEditor " + buildVersion();

    private final EditorTabs tabs = new EditorTabs();
    private final FindingsPanel findings = new FindingsPanel();
    private final ToolbarPanel toolbar;
    // Nicht final, weil die Aktionen der Symbolleiste (Lambdas) sie erst nach dem Aufbau des Fensters benutzen
    private DocumentController documents;
    private CheckController checks;
    private SpecificationController specifications;

    // Die Lambdas lösen documents/checks bewusst erst beim Aufruf auf; Methodenreferenzen würden das noch leere Feld binden
    @SuppressWarnings("PMD.LambdaCanBeMethodReference")
    public MainFrame(SpecificationStore store) {
        super(APP_TITLE);
        setIconImages(Icons.appIcons());
        toolbar = new ToolbarPanel(() -> documents.openWithDialog(), this::buildMenu, () -> documents.saveActive(),
                () -> documents.saveActiveAs(), () -> specifications.edit(), () -> checks.run());
        SwingDialogs dialogs = new SwingDialogs(this);
        documents = new DocumentController(tabs, new RecentFiles(3), dialogs, toolbar::setStatus);
        specifications = new SpecificationController(store, dialogs, toolbar::setSchemaOptions, toolbar::setStatus);
        checks = new CheckController(() -> specifications.service(), tabs, toolbar, this, this::showResult);

        tabs.onCloseRequested(documents::close);
        tabs.onChange(this::updateTitle);
        tabs.onActiveTabChanged(this::showResult);
        findings.onSelected(finding -> {
            if (finding.hasPosition()) {
                tabs.active().goToLine(finding.line());
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, findings);
        split.setResizeWeight(0.7);
        add(toolbar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        tabs.addTab();

        setPreferredSize(new Dimension(1100, 800));
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                closeApplication();
            }
        });
    }

    /** Version aus dem Manifest des Jars (vom Build gesetzt); beim Start aus der IDE gibt es sie nicht. */
    private static String buildVersion() {
        String version = MainFrame.class.getPackage().getImplementationVersion();
        return version == null ? "(Entwicklung)" : version;
    }

    /** Dropdown des Öffnen-Buttons: Dateiaktionen und darunter das Einrichten der Spezifikationen. */
    private JPopupMenu buildMenu() {
        JPopupMenu menu = documents.buildFileMenu();
        menu.addSeparator();
        JMenuItem setup = new JMenuItem("Spezifikationen…");
        setup.addActionListener(e -> specifications.edit());
        menu.add(setup);
        return menu;
    }

    /** Öffnet die Dateien (z. B. aus der Kommandozeile) jeweils in einem eigenen Tab. */
    public void openAll(List<Path> files) {
        documents.openAll(files);
    }

    /** Zeigt Befunde und Status des Tabs. */
    private void showResult(EditorTab tab) {
        findings.show(tab.findings());
        toolbar.setStatus(tab.checkStatus());
    }

    private void updateTitle() {
        EditorTab current = tabs.active();
        setTitle(current == null ? APP_TITLE
                : APP_TITLE + " – " + current.tooltip() + (current.isDirty() ? " *" : ""));
    }

    /** Beendet die Anwendung; vorher wird für jeden Tab mit ungespeicherten Änderungen nachgefragt. */
    private void closeApplication() {
        if (documents.confirmDiscardAll()) {
            dispose();
            System.exit(0);
        }
    }
}
