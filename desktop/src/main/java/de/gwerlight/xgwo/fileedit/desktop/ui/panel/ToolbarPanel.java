package de.gwerlight.xgwo.fileedit.desktop.ui.panel;

import de.gwerlight.xgwo.fileedit.desktop.config.SchemaOption;
import de.gwerlight.xgwo.fileedit.desktop.ui.component.Icons;
import de.gwerlight.xgwo.fileedit.desktop.ui.component.SplitButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;

/**
 * Symbolleiste mit Icons: Öffnen (mit Dropdown), Speichern, Speichern unter, Spezifikationen, Prüfen, Schema-Auswahl,
 * Fortschritt und Statuszeile.
 */
public final class ToolbarPanel extends JPanel {

    private final JButton checkButton = new JButton("Prüfen", Icons.green("check"));
    private final JComboBox<SchemaOption> schemaChoice = new JComboBox<>() {
        @Override public Dimension getMaximumSize() {
            return getPreferredSize(); // sonst zieht das BoxLayout der Symbolleiste die Auswahl in die Breite
        }
    };
    private final JProgressBar progress = new JProgressBar();
    private final JLabel status = new JLabel(" ");

    /** Die Aktionen werden beim Klick aufgerufen; {@code fileMenu} liefert das Dropdown des Öffnen-Buttons. */
    public ToolbarPanel(Runnable onOpen, Supplier<JPopupMenu> fileMenu, Runnable onSave, Runnable onSaveAs,
                        Runnable onSpecifications, Runnable onCheck) {
        super(new BorderLayout());
        checkButton.setToolTipText("XML prüfen (XSD und Schematron)");
        checkButton.addActionListener(e -> onCheck.run());
        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.setMaximumSize(new Dimension(80, progress.getPreferredSize().height));

        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(new SplitButton(Icons.of("open"), "Öffnen…", e -> onOpen.run(), fileMenu));
        bar.add(button("save", "Speichern", onSave));
        bar.add(button("save-as", "Speichern unter…", onSaveAs));
        bar.addSeparator();
        bar.add(checkButton);
        bar.addSeparator();
        bar.add(new JLabel("Schema: "));
        bar.add(schemaChoice);
        bar.add(button("specifications", "Spezifikationen einrichten…", onSpecifications));
        bar.addSeparator();
        bar.add(progress);
        bar.add(Box.createHorizontalStrut(6));
        bar.add(status);
        add(bar, BorderLayout.CENTER);
    }

    private static JButton button(String icon, String tooltip, Runnable action) {
        Icon image = Icons.of(icon);
        JButton button = new JButton(image);
        button.setToolTipText(tooltip);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.addActionListener(e -> action.run());
        return button;
    }

    /** Ersetzt die Schema-Auswahl; die bisherige Wahl bleibt erhalten, wenn es sie noch gibt. */
    public void setSchemaOptions(List<SchemaOption> options) {
        SchemaOption previous = selectedSchema();
        schemaChoice.removeAllItems();
        options.forEach(schemaChoice::addItem);
        options.stream().filter(o -> previous != null && o.label().equals(previous.label())).findFirst()
                .ifPresent(schemaChoice::setSelectedItem);
    }

    public SchemaOption selectedSchema() {
        return (SchemaOption) schemaChoice.getSelectedItem();
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    /** Sperrt während einer laufenden Prüfung den Button und zeigt den Fortschrittsbalken. */
    public void setBusy(boolean busy) {
        checkButton.setEnabled(!busy);
        progress.setVisible(busy);
    }
}
