package de.gwerlight.xgwo.fileedit.desktop.ui.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.UIManager;

/** SVG-Icons der Symbolleiste (Ordner {@code icons}); die Farbe kommt zur Laufzeit aus dem Look-and-Feel. */
public final class Icons {

    private static final int SIZE = 19;
    private static final Color GREEN = new Color(0x2E7D32);

    private Icons() { }

    /** Programm-Icon in mehreren Größen für Fenster und Taskleiste; leer, wenn die Bilder nicht ladbar sind. */
    public static List<Image> appIcons() {
        List<Image> images = new ArrayList<>();
        for (int size : new int[] {32, 64, 256}) {
            try (InputStream in = Icons.class.getResourceAsStream("/icons/app-" + size + ".png")) {
                if (in != null) {
                    images.add(ImageIO.read(in));
                }
            } catch (IOException e) {
                return List.of();
            }
        }
        return images;
    }

    /** Icon in der Textfarbe des Themes (kräftiger als die blasse Aktionsfarbe „Actions.Grey“). */
    public static Icon of(String name) {
        return of(name, () -> UIManager.getColor("Button.foreground"));
    }

    /** Icon in kräftigem Grün, z. B. für die Hauptaktion „Prüfen“. */
    public static Icon green(String name) {
        return of(name, () -> GREEN);
    }

    private static Icon of(String name, Supplier<Color> color) {
        // Die Farbe wird erst beim Zeichnen gelesen, damit ein Theme-Wechsel die Icons mitnimmt
        return new FlatSVGIcon("icons/" + name + ".svg", SIZE, SIZE)
                .setColorFilter(new FlatSVGIcon.ColorFilter(c -> color.get()));
    }
}
