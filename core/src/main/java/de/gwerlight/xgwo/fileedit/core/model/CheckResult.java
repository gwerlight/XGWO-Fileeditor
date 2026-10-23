package de.gwerlight.xgwo.fileedit.core.model;

import java.util.List;

/**
 * Ergebnis einer Prüfung.
 *
 * @param schemaId verwendete Schema-ID oder {@code null}, wenn keines gefunden wurde
 * @param findings alle Befunde in Prüfreihenfolge
 */
public record CheckResult(String schemaId, List<Finding> findings) {

    public CheckResult {
        findings = List.copyOf(findings);
    }

    public boolean isValid() {
        return errorCount() == 0;
    }

    public long errorCount() {
        return findings.stream().filter(f -> f.severity() == Severity.ERROR).count();
    }
}
