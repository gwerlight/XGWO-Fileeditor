package de.gwerlight.xgwo.fileedit.desktop;

import com.formdev.flatlaf.FlatLightLaf;
import de.gwerlight.xgwo.fileedit.desktop.spec.SpecificationStore;
import de.gwerlight.xgwo.fileedit.desktop.ui.MainFrame;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

/** Startpunkt der Desktop-Anwendung; Dateien als Argumente (z. B. „Öffnen mit“ im Explorer) öffnen je einen Tab. */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        List<Path> files = Arrays.stream(args).map(Path::of).toList();
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(SpecificationStore.standard());
            frame.setVisible(true);
            frame.openAll(files);
        });
    }
}
