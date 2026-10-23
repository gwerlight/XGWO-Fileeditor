package de.gwerlight.xgwo.fileedit.desktop.ui.panel;

import de.gwerlight.xgwo.fileedit.core.model.Finding;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Tabelle der Prüfbefunde (Schweregrad, Zeile, Meldung). */
public final class FindingsPanel extends JScrollPane {

    private final DefaultTableModel model = new DefaultTableModel(new Object[] {"", "Zeile", "Meldung"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<Finding> shown = List.of();

    public FindingsPanel() {
        setViewportView(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(30);
        table.getColumnModel().getColumn(1).setMaxWidth(70);
    }

    /** Wird aufgerufen, wenn der Benutzer einen Befund auswählt (z. B. um zur Fundstelle zu springen). */
    public void onSelected(Consumer<Finding> listener) {
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && row < shown.size()) {
                listener.accept(shown.get(row));
            }
        });
    }

    public void show(List<Finding> findings) {
        shown = List.copyOf(findings);
        model.setRowCount(0);
        for (Finding f : shown) {
            model.addRow(new Object[] {
                    switch (f.severity()) { case ERROR -> "✖"; case WARNING -> "⚠"; case INFO -> "ℹ"; },
                    f.hasPosition() ? f.line() : "",
                    f.message()});
        }
    }
}
