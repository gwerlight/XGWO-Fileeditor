package de.gwerlight.xgwo.fileedit.desktop.ui.component;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * Symbolleisten-Button mit Standardaktion und angehängtem Dropdown-Pfeil. Der Hauptteil löst die Standardaktion aus,
 * der Pfeil öffnet ein Menü mit weiteren Aktionen. Das Menü wird bei jedem Öffnen neu erzeugt,
 * damit es z. B. die aktuelle Liste der zuletzt geöffneten Dateien zeigen kann.
 */
public final class SplitButton extends JPanel {

    private static final String TOOLBAR_STYLE = "toolBarButton";

    public SplitButton(Icon icon, String tooltip, ActionListener defaultAction, Supplier<JPopupMenu> menu) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        JButton main = new JButton(icon);
        main.setToolTipText(tooltip);
        main.putClientProperty("JButton.buttonType", TOOLBAR_STYLE);
        main.addActionListener(defaultAction);
        JButton arrow = new JButton("▾");
        arrow.setToolTipText("Zuletzt geöffnet und weitere Aktionen");
        arrow.putClientProperty("JButton.buttonType", TOOLBAR_STYLE);
        arrow.setFocusable(false);
        arrow.addActionListener(e -> menu.get().show(this, 0, getHeight()));
        add(main);
        add(arrow);
        // In einer Symbolleiste (BoxLayout) soll der Button nicht auseinandergezogen werden
        setMaximumSize(new Dimension(getPreferredSize()));
    }
}
