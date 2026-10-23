package de.gwerlight.xgwo.fileedit.desktop.ui.tabs;

import com.formdev.flatlaf.FlatClientProperties;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JTabbedPane;

/** Die Tabs mit den geöffneten Dokumenten. Nach dem Start ist immer mindestens ein Tab vorhanden. */
public final class EditorTabs extends JTabbedPane {

    private Runnable changeListener = () -> { };
    private Consumer<EditorTab> activeListener = tab -> { };

    public EditorTabs() {
        setTabLayoutPolicy(SCROLL_TAB_LAYOUT);
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Tab schließen");
        addChangeListener(e -> {
            refresh();
            EditorTab current = active();
            if (current != null) {
                activeListener.accept(current);
                current.editor().requestFocusInWindow();
            }
        });
    }

    /** Wird aufgerufen, wenn der Benutzer das ✕ eines Tabs anklickt; das Schließen selbst übernimmt der Aufrufer. */
    public void onCloseRequested(Consumer<EditorTab> handler) {
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK,
                (BiConsumer<JTabbedPane, Integer>) (pane, index) -> handler.accept(tabAt(index)));
    }

    /** Wird aufgerufen, wenn sich Titel oder Änderungsstand eines Tabs ändern. */
    public void onChange(Runnable listener) { this.changeListener = listener; }

    /** Wird aufgerufen, wenn ein anderer Tab aktiv wird. */
    public void onActiveTabChanged(Consumer<EditorTab> listener) { this.activeListener = listener; }

    public EditorTab active() {
        return (EditorTab) getSelectedComponent();
    }

    public EditorTab addTab() {
        EditorTab tab = new EditorTab(this::refresh);
        addTab(tab.title(), tab);
        setSelectedComponent(tab);
        refresh();
        return tab;
    }

    public List<EditorTab> all() {
        List<EditorTab> tabs = new ArrayList<>();
        for (int i = 0; i < getTabCount(); i++) {
            tabs.add(tabAt(i));
        }
        return tabs;
    }

    public Optional<EditorTab> find(Path file) {
        return all().stream().filter(t -> file.equals(t.file())).findFirst();
    }

    public void select(EditorTab tab) {
        setSelectedComponent(tab);
    }

    /** Entfernt den Tab ohne Rückfrage; es bleibt immer ein (leeres) Dokument zum Einfügen/Tippen übrig. */
    public void closeTab(EditorTab tab) {
        remove(tab);
        if (getTabCount() == 0) {
            addTab();
        }
    }

    /** Aktualisiert Tab-Titel (inkl. „*“ bei Änderungen) und benachrichtigt den Besitzer, z. B. für den Fenstertitel. */
    private void refresh() {
        for (int i = 0; i < getTabCount(); i++) {
            setTitleAt(i, tabAt(i).title());
            setToolTipTextAt(i, tabAt(i).tooltip());
        }
        changeListener.run();
    }

    private EditorTab tabAt(int index) {
        return (EditorTab) getComponentAt(index);
    }
}
