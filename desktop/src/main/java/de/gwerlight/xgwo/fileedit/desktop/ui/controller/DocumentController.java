package de.gwerlight.xgwo.fileedit.desktop.ui.controller;

import de.gwerlight.xgwo.fileedit.desktop.settings.RecentFiles;
import de.gwerlight.xgwo.fileedit.desktop.ui.tabs.EditorTab;
import de.gwerlight.xgwo.fileedit.desktop.ui.tabs.EditorTabs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/** Dateioperationen auf den Tabs: Öffnen, Speichern, Schließen mit Rückfrage und die Liste zuletzt geöffneter Dateien. */
public final class DocumentController {

    private final EditorTabs tabs;
    private final RecentFiles recent;
    private final Dialogs dialogs;
    private final Consumer<String> status;

    public DocumentController(EditorTabs tabs, RecentFiles recent, Dialogs dialogs, Consumer<String> status) {
        this.tabs = tabs;
        this.recent = recent;
        this.dialogs = dialogs;
        this.status = status;
    }

    // ---- Öffnen ----

    public void openWithDialog() {
        dialogs.chooseFilesToOpen(startDirectory()).forEach(this::open);
    }

    /** Öffnet die Dateien (z. B. aus der Kommandozeile) jeweils in einem eigenen Tab. */
    public void openAll(List<Path> files) {
        files.forEach(this::open);
    }

    /** Öffnet die Datei in einem neuen Tab; ist sie schon offen, wird deren Tab nur nach vorne geholt. */
    private void open(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        var existing = tabs.find(normalized);
        if (existing.isPresent()) {
            tabs.select(existing.get());
            return;
        }
        String text;
        try {
            text = Files.readString(normalized, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            recent.remove(normalized);
            dialogs.showError("Datei nicht lesbar", ex.getMessage());
            return;
        }
        // Ein leerer, unberührter Tab (z. B. der beim Start) wird wiederverwendet statt zusätzlich einen zu öffnen
        EditorTab tab = tabs.active() != null && tabs.active().isUntouched() ? tabs.active() : tabs.addTab();
        tab.loaded(normalized, text);
        recent.add(normalized);
    }

    // ---- Speichern ----

    public void saveActive() {
        save(tabs.active());
    }

    public void saveActiveAs() {
        saveAs(tabs.active());
    }

    /** @return {@code true}, wenn gespeichert wurde */
    private boolean save(EditorTab tab) {
        return tab.file() == null ? saveAs(tab) : write(tab, tab.file());
    }

    /** @return {@code true}, wenn gespeichert wurde */
    private boolean saveAs(EditorTab tab) {
        var chosen = dialogs.chooseSaveTarget(startDirectory(), tab.file());
        if (chosen.isEmpty()) {
            return false;
        }
        Path target = chosen.get().toAbsolutePath().normalize();
        if (Files.exists(target) && !target.equals(tab.file()) && !dialogs.confirmOverwrite(target)) {
            return false;
        }
        return write(tab, target);
    }

    private boolean write(EditorTab tab, Path file) {
        try {
            Files.writeString(file, tab.editor().getText(), StandardCharsets.UTF_8);
            tab.markSaved(file);
            recent.add(file);
            status.accept("Gespeichert: " + file.getFileName());
            return true;
        } catch (IOException ex) {
            dialogs.showError("Datei nicht speicherbar", ex.getMessage());
            return false;
        }
    }

    // ---- Schließen ----

    /** Schließt den Tab; bei ungespeicherten Änderungen wird vorher nachgefragt. */
    public void close(EditorTab tab) {
        if (confirmDiscard(tab)) {
            tabs.closeTab(tab);
        }
    }

    /** @return {@code true}, wenn für alle Tabs geklärt ist, was mit Änderungen passiert, und die Anwendung enden darf */
    public boolean confirmDiscardAll() {
        for (EditorTab tab : tabs.all()) {
            if (!confirmDiscard(tab)) {
                return false;
            }
        }
        return true;
    }

    /** Fragt bei ungespeicherten Änderungen nach. @return {@code false}, wenn der Vorgang abgebrochen werden soll */
    private boolean confirmDiscard(EditorTab tab) {
        if (!tab.isDirty()) {
            return true;
        }
        tabs.select(tab); // der Benutzer soll sehen, um welches Dokument es geht
        return switch (dialogs.askSaveChanges(tab.title().replace(" *", ""))) {
            case SAVE -> save(tab);
            case DISCARD -> true;
            case CANCEL -> false;
        };
    }

    // ---- Menü ----

    /** Dropdown des Öffnen-Buttons: Speichern-Aktionen (aktiver Tab) und die zuletzt verwendeten Dateien. */
    public JPopupMenu buildFileMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem save = new JMenuItem("Speichern");
        save.addActionListener(e -> saveActive());
        JMenuItem saveAs = new JMenuItem("Speichern unter…");
        saveAs.addActionListener(e -> saveActiveAs());
        menu.add(save);
        menu.add(saveAs);
        menu.addSeparator();
        JMenuItem header = new JMenuItem("Zuletzt geöffnet");
        header.setEnabled(false);
        menu.add(header);
        List<Path> files = recent.list();
        if (files.isEmpty()) {
            JMenuItem empty = new JMenuItem("(keine)");
            empty.setEnabled(false);
            menu.add(empty);
        }
        for (Path file : files) {
            JMenuItem item = new JMenuItem(file.getFileName() + "  –  " + file.getParent());
            item.addActionListener(e -> open(file));
            menu.add(item);
        }
        return menu;
    }

    private Path startDirectory() {
        EditorTab current = tabs.active();
        return current == null || current.file() == null ? null : current.file().getParent();
    }
}
