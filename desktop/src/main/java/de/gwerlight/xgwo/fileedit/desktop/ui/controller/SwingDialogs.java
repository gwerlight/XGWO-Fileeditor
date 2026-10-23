package de.gwerlight.xgwo.fileedit.desktop.ui.controller;

import de.gwerlight.xgwo.fileedit.desktop.spec.Specification;
import de.gwerlight.xgwo.fileedit.desktop.ui.dialog.SpecificationsDialog;
import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/** Dialoge als Swing-Standarddialoge ({@link JFileChooser}, {@link JOptionPane}) über dem übergebenen Fenster. */
public final class SwingDialogs implements Dialogs {

    private final Component parent;

    public SwingDialogs(Component parent) {
        this.parent = parent;
    }

    @Override
    public List<Path> chooseFilesToOpen(Path startDirectory) {
        JFileChooser chooser = new JFileChooser(startDirectory == null ? null : startDirectory.toFile());
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return List.of();
        }
        return Arrays.stream(chooser.getSelectedFiles()).map(File::toPath).toList();
    }

    @Override
    public Optional<Path> chooseSaveTarget(Path startDirectory, Path current) {
        JFileChooser chooser = new JFileChooser(startDirectory == null ? null : startDirectory.toFile());
        if (current != null) {
            chooser.setSelectedFile(current.toFile());
        }
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        return Optional.of(chooser.getSelectedFile().toPath());
    }

    @Override
    public boolean confirmOverwrite(Path target) {
        return JOptionPane.showConfirmDialog(parent, target.getFileName() + " existiert bereits. Überschreiben?",
                "Speichern unter", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    @Override
    public SaveChoice askSaveChanges(String documentName) {
        int choice = JOptionPane.showOptionDialog(parent,
                "„" + documentName + "“ hat ungespeicherte Änderungen. Wollen Sie speichern?",
                "Ungespeicherte Änderungen", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[] {"Speichern", "Nicht speichern", "Abbrechen"}, "Speichern");
        return switch (choice) {
            case 0 -> SaveChoice.SAVE;
            case 1 -> SaveChoice.DISCARD;
            default -> SaveChoice.CANCEL;
        };
    }

    @Override
    public Optional<List<Specification>> editSpecifications(List<Specification> current) {
        return new SpecificationsDialog(parent, current).showDialog();
    }

    @Override
    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
