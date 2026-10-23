package de.gwerlight.xgwo.fileedit.desktop.ui.component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;

/**
 * Auswahl einer Version mit einer Nachkommastelle, in Zehnerschritten: 1.6, 1.7, 1.8, 1.9, 2.0, 2.1 … Der Wert wird mit
 * Punkt angezeigt, unabhängig von der Spracheinstellung.
 */
public final class VersionSpinner extends JSpinner {

    private static final BigDecimal STEP = new BigDecimal("0.1");
    private static final BigDecimal MIN = new BigDecimal("0.1");
    private static final BigDecimal MAX = new BigDecimal("99.9");

    public VersionSpinner() {
        super(new TenthsModel());
        NumberEditor editor = new NumberEditor(this, "0.0");
        // Punkt statt Komma, sonst würde eine deutsche Oberfläche „1,6“ anzeigen
        editor.getFormat().setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
        ((DefaultFormatter) editor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
        editor.getTextField().setValue(getValue()); // der Startwert wurde noch mit dem alten Format angezeigt
        setEditor(editor);
    }

    /** Die Version als Text mit einer Nachkommastelle, z. B. {@code 1.6}. */
    public String version() {
        return BigDecimal.valueOf(((Number) getValue()).doubleValue()).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    /** Setzt die Version; Text, der keine Zahl ist (oder außerhalb des Bereichs liegt), wird ignoriert. */
    public void setVersion(String text) {
        parse(text).filter(v -> v.compareTo(MIN) >= 0 && v.compareTo(MAX) <= 0)
                .ifPresent(v -> setValue(v.doubleValue()));
    }

    private static Optional<BigDecimal> parse(String text) {
        try {
            return text == null ? Optional.empty() : Optional.of(new BigDecimal(text.strip()).setScale(1, RoundingMode.HALF_UP));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Rechnet mit {@link BigDecimal}, damit aus 1.6 mit Schritt 0.1 nicht 1.7000000000000002 wird. */
    private static final class TenthsModel extends SpinnerNumberModel {

        TenthsModel() {
            super(1.6, MIN.doubleValue(), MAX.doubleValue(), STEP.doubleValue());
        }

        @Override public Object getNextValue() {
            BigDecimal next = current().add(STEP);
            return next.compareTo(MAX) > 0 ? null : next.doubleValue();
        }

        @Override public Object getPreviousValue() {
            BigDecimal previous = current().subtract(STEP);
            return previous.compareTo(MIN) < 0 ? null : previous.doubleValue();
        }

        private BigDecimal current() {
            return BigDecimal.valueOf(getNumber().doubleValue()).setScale(1, RoundingMode.HALF_UP);
        }
    }
}
