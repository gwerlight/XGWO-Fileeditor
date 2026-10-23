package de.gwerlight.xgwo.fileedit.desktop.ui.tabs;

import de.gwerlight.xgwo.fileedit.core.model.Finding;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/** Ein geöffnetes Dokument: Editor, zugehörige Datei, Änderungsstand und das letzte Prüfergebnis. */
public final class EditorTab extends JPanel {

    private static final String UNTITLED = "Unbenannt";

    private final RSyntaxTextArea editor = new RSyntaxTextArea(30, 100);
    private final Runnable onChange;
    private Path file;
    private boolean dirty;
    private List<Finding> findings = List.of();
    private String checkStatus = " ";

    /** @param onChange wird aufgerufen, wenn sich Titel oder Änderungsstand ändern */
    public EditorTab(Runnable onChange) {
        super(new BorderLayout());
        this.onChange = onChange;
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        editor.setCodeFoldingEnabled(true);
        editor.setTabSize(2);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { markDirty(); }
            @Override public void removeUpdate(DocumentEvent e) { markDirty(); }
            @Override public void changedUpdate(DocumentEvent e) { markDirty(); }
        });
        add(new RTextScrollPane(editor), BorderLayout.CENTER);
    }

    public RSyntaxTextArea editor() { return editor; }

    /** Zugehörige Datei oder {@code null}, solange das Dokument nie gespeichert/geladen wurde. */
    public Path file() { return file; }

    public boolean isDirty() { return dirty; }

    /** Neues, leeres und unverändertes Dokument, das beim Öffnen einer Datei wiederverwendet werden kann. */
    public boolean isUntouched() { return file == null && !dirty && editor.getDocument().getLength() == 0; }

    public String title() { return (file == null ? UNTITLED : displayName(file)) + (dirty ? " *" : ""); }

    /** Dateiname; für Pfade ohne Namen (z. B. eine Wurzel) der ganze Pfad. */
    private static String displayName(Path path) {
        Path name = path.getFileName();
        return name == null ? path.toString() : name.toString();
    }

    public String tooltip() { return file == null ? UNTITLED : file.toString(); }

    /** Setzt den Dateiinhalt und merkt die Datei; das Dokument gilt danach als gespeichert. */
    public void loaded(Path path, String text) {
        editor.setText(text);
        editor.setCaretPosition(0);
        markSaved(path);
    }

    public void markSaved(Path path) {
        file = path.toAbsolutePath().normalize();
        dirty = false;
        onChange.run();
    }

    private void markDirty() {
        if (!dirty) {
            dirty = true;
            onChange.run();
        }
    }

    /** Setzt den Cursor an den Anfang der Zeile (1-basiert) und gibt dem Editor den Fokus. */
    public void goToLine(int line) {
        try {
            editor.setCaretPosition(editor.getLineStartOffset(line - 1));
            editor.requestFocusInWindow();
        } catch (BadLocationException ignored) {
            // Zeile existiert nicht (mehr), z. B. weil der Text inzwischen geändert wurde
        }
    }

    public List<Finding> findings() { return findings; }

    public String checkStatus() { return checkStatus; }

    public void setCheckResult(List<Finding> findings, String status) {
        this.findings = List.copyOf(findings);
        this.checkStatus = status;
    }
}
