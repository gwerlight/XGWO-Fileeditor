package de.gwerlight.xgwo.fileedit.desktop.ui.dialog;

import de.gwerlight.xgwo.fileedit.desktop.spec.Specification;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Liste der eingerichteten Spezifikationen mit Hinzufügen, Bearbeiten und Entfernen. Änderungen gelten erst mit „OK“.
 * Die Dateien selbst bleiben unangetastet, es werden nur ihre Pfade gemerkt.
 */
public final class SpecificationsDialog extends JDialog {

    private final List<Specification> items;
    private final DefaultTableModel model = new DefaultTableModel(new String[] {"Name", "Version", "XSD", "Schematron-XSL"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton edit = new JButton("Bearbeiten…");
    private final JButton remove = new JButton("Entfernen");
    private List<Specification> result;

    public SpecificationsDialog(Component parent, List<Specification> current) {
        super(SwingUtilities.getWindowAncestor(parent), "Spezifikationen", ModalityType.APPLICATION_MODAL);
        items = new ArrayList<>(current);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editSelected();
                }
            }
        });
        int[] widths = {140, 70, 270, 270};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(760, 220));

        JButton add = new JButton("Hinzufügen…");
        add.addActionListener(e -> addNew());
        edit.addActionListener(e -> editSelected());
        remove.addActionListener(e -> removeSelected());
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        for (JButton b : List.of(add, edit, remove)) {
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
            side.add(b);
            side.add(Box.createVerticalStrut(6));
        }

        JButton cancel = new JButton("Abbrechen");
        cancel.addActionListener(e -> dispose());
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            result = List.copyOf(items);
            dispose();
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);

        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        center.add(scroll, BorderLayout.CENTER);
        center.add(side, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);

        refresh();
        pack();
        setLocationRelativeTo(parent);
    }

    /** @return die geänderte Liste, leer bei Abbruch */
    public Optional<List<Specification>> showDialog() {
        setVisible(true);
        return Optional.ofNullable(result);
    }

    private void addNew() {
        new SpecificationEditDialog(this, null).showDialog().ifPresent(spec -> {
            items.add(spec);
            refresh();
            table.setRowSelectionInterval(items.size() - 1, items.size() - 1);
        });
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        new SpecificationEditDialog(this, items.get(row)).showDialog().ifPresent(spec -> {
            items.set(row, spec);
            refresh();
            table.setRowSelectionInterval(row, row);
        });
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            items.remove(row);
            refresh();
        }
    }

    private void refresh() {
        model.setRowCount(0);
        for (Specification s : items) {
            model.addRow(new Object[] {s.name(), s.version(), s.xsd(), s.xsl()});
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean selected = table.getSelectedRow() >= 0;
        edit.setEnabled(selected);
        remove.setEnabled(selected);
    }
}
