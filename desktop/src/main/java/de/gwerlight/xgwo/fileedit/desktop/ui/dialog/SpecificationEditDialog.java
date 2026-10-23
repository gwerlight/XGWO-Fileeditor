package de.gwerlight.xgwo.fileedit.desktop.ui.dialog;

import de.gwerlight.xgwo.fileedit.desktop.spec.Specification;
import de.gwerlight.xgwo.fileedit.desktop.spec.SpecificationChecks;
import de.gwerlight.xgwo.fileedit.desktop.ui.component.VersionSpinner;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Eingabe einer Spezifikation: Name, Version und die Pfade zu XSD und Schematron-XSL, mit laufender Prüfung. */
final class SpecificationEditDialog extends JDialog {

    private static final int MAX_MESSAGE = 300;
    // Kräftiges Rot; die Fehlerfarbe des Themes ist für Text zu blass
    private static final Color ERROR_COLOR = new Color(0xC62828);

    private final JTextField name = new JTextField(30);
    private final VersionSpinner version = new VersionSpinner();
    private final JTextField xsd = new JTextField(40);
    private final JTextField xsl = new JTextField(40);
    private final JLabel message = new JLabel(" ");
    private final JButton ok = new JButton("OK");
    private Specification result;

    /** @param initial vorbelegte Werte beim Bearbeiten, {@code null} für einen neuen Eintrag */
    SpecificationEditDialog(Window owner, Specification initial) {
        super(owner, initial == null ? "Spezifikation hinzufügen" : "Spezifikation bearbeiten", ModalityType.APPLICATION_MODAL);
        if (initial != null) {
            name.setText(initial.name());
            version.setVersion(initial.version());
            xsd.setText(initial.xsd().toString());
            xsl.setText(initial.xsl().toString());
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        // Der Name kommt aus der XSD und wird nicht von Hand eingegeben
        name.setEditable(false);
        name.setFocusable(false);
        addRow(form, 0, "Name (aus der XSD):", name, null);
        JPanel versionBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        versionBox.add(version);
        addRow(form, 1, "Version:", versionBox, null);
        addRow(form, 2, "XSD (Hauptdatei):", xsd, chooseButton(xsd, "XML-Schema (*.xsd)", "xsd"));
        addRow(form, 3, "Schematron-XSL:", xsl, chooseButton(xsl, "XSLT (*.xsl, *.xslt)", "xsl", "xslt"));
        message.setForeground(ERROR_COLOR);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 0, 0, 0);
        form.add(message, c);

        JButton cancel = new JButton("Abbrechen");
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> accept());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);

        DocumentListener validate = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validateFields(); }
            @Override public void removeUpdate(DocumentEvent e) { validateFields(); }
            @Override public void changedUpdate(DocumentEvent e) { validateFields(); }
        };
        version.addChangeListener(e -> validateFields());
        xsd.getDocument().addDocumentListener(validate);
        xsl.getDocument().addDocumentListener(validate);
        validateFields();

        pack();
        setMinimumSize(new Dimension(getWidth(), getHeight()));
        setLocationRelativeTo(owner);
    }

    /** @return die eingegebene Spezifikation, leer bei Abbruch */
    Optional<Specification> showDialog() {
        setVisible(true);
        return Optional.ofNullable(result);
    }

    private void accept() {
        // Das Laden der XSD dauert bei großen Schemas einen Moment und steht deshalb nicht in der laufenden Prüfung
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Optional<String> error;
        try {
            error = SpecificationChecks.loadError(path(xsd));
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
        if (error.isPresent()) {
            showMessage("Die XSD lässt sich nicht laden: " + error.get());
            return;
        }
        result = new Specification(name.getText().strip(), version.version(), path(xsd), path(xsl));
        dispose();
    }

    /** Die gesetzte Version steuert die Prüfung; der Name aus der XSD wird nur bei bestandener Prüfung übernommen. */
    private void validateFields() {
        List<String> problems = SpecificationChecks.problems(version.version(), path(xsd), path(xsl));
        name.setText(problems.isEmpty() ? SpecificationChecks.specificationName(path(xsd)) : "");
        showMessage(problems.isEmpty() ? " " : problems.get(0));
        ok.setEnabled(problems.isEmpty());
    }

    private void showMessage(String text) {
        String shown = text.length() > MAX_MESSAGE ? text.substring(0, MAX_MESSAGE) + "…" : text;
        message.setText("<html><body style='width: 420px'>" + shown.replace("&", "&amp;").replace("<", "&lt;") + "</body></html>");
    }

    private static Path path(JTextField field) {
        String text = field.getText().strip();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Path.of(text);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private JButton chooseButton(JTextField target, String description, String... extensions) {
        JButton button = new JButton("…");
        button.addActionListener(e -> {
            Path current = path(target);
            JFileChooser chooser = new JFileChooser(current == null || current.getParent() == null ? null : current.getParent().toFile());
            chooser.setFileFilter(new FileNameExtensionFilter(description, extensions));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                target.setText(chooser.getSelectedFile().toPath().toString());
            }
        });
        return button;
    }

    private static void addRow(JPanel form, int row, String label, JComponent field, JButton button) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(3, 0, 3, 6);
        c.anchor = GridBagConstraints.WEST;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
        if (button != null) {
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            form.add(button, c);
        }
    }
}
