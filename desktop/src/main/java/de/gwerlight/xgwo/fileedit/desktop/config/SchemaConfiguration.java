package de.gwerlight.xgwo.fileedit.desktop.config;

import de.gwerlight.xgwo.fileedit.core.schema.NamespaceSchemaCatalog;
import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle;
import de.gwerlight.xgwo.fileedit.core.service.DefaultXmlCheckService;
import de.gwerlight.xgwo.fileedit.core.service.XmlCheckService;
import de.gwerlight.xgwo.fileedit.desktop.spec.Specification;
import de.gwerlight.xgwo.fileedit.desktop.spec.SpecificationChecks;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verfügbare Schemas und der daraus aufgebaute Prüfdienst, abgeleitet aus den eingerichteten Spezifikationen.
 *
 * @param service  Prüfdienst mit automatischer Schema-Erkennung über den Namespace des Wurzelelements
 * @param options  Einträge der Schema-Auswahl (erster Eintrag: automatische Erkennung)
 * @param problems Spezifikationen, die nicht verwendet werden konnten, mit Begründung
 */
public record SchemaConfiguration(XmlCheckService service, List<SchemaOption> options, List<String> problems) {

    public SchemaConfiguration {
        options = List.copyOf(options);
        problems = List.copyOf(problems);
    }

    /** Baut die Konfiguration aus den Spezifikationen; unbrauchbare werden übersprungen und unter {@code problems} genannt. */
    public static SchemaConfiguration from(List<Specification> specifications) {
        List<SchemaOption> options = new ArrayList<>();
        options.add(SchemaOption.automatic());
        List<String> problems = new ArrayList<>();
        Map<String, SchemaBundle> byNamespace = new HashMap<>();
        Map<String, String> owner = new HashMap<>();
        for (Specification spec : specifications) {
            List<String> found = SpecificationChecks.problems(spec.version(), spec.xsd(), spec.xsl());
            if (!found.isEmpty()) {
                problems.add(spec.label() + ": " + found.get(0));
                continue;
            }
            SchemaBundle bundle;
            try {
                bundle = new SchemaBundle(spec.label(), spec.xsd().toUri().toURL(), List.of(spec.xsl().toUri().toURL()));
            } catch (MalformedURLException e) {
                problems.add(spec.label() + ": " + e.getMessage());
                continue;
            }
            options.add(new SchemaOption(spec.label(), bundle));
            String namespace = SpecificationChecks.targetNamespace(spec.xsd()).orElse("");
            String other = owner.putIfAbsent(namespace, spec.label());
            if (other == null) {
                byNamespace.put(namespace, bundle);
            } else {
                problems.add(spec.label() + ": Der Namespace wird schon von '" + other
                        + "' belegt, die automatische Erkennung verwendet '" + other + "'.");
            }
        }
        return new SchemaConfiguration(
                DefaultXmlCheckService.withDefaults(new NamespaceSchemaCatalog(byNamespace)), options, problems);
    }
}
