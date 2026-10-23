package de.gwerlight.xgwo.fileedit.core.check;

import de.gwerlight.xgwo.fileedit.core.model.Finding;
import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle;

import java.util.List;

/** Ein Prüfschritt (XSD, Schematron, später z. B. eigene Mapping-Regeln). Schritte laufen nacheinander. */
public interface CheckStep {
    List<Finding> run(String xml, SchemaBundle bundle);
}
