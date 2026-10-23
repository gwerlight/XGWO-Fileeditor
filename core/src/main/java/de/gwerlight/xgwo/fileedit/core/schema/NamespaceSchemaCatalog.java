package de.gwerlight.xgwo.fileedit.core.schema;

import java.util.Map;
import java.util.Optional;

/** Ordnet das Schema über den Namespace des Wurzelelements zu (z. B. {@code .../xga/1.6} → XGWO 1.6). */
public final class NamespaceSchemaCatalog implements SchemaCatalog {

    private final Map<String, SchemaBundle> byNamespace;

    public NamespaceSchemaCatalog(Map<String, SchemaBundle> byNamespace) {
        this.byNamespace = Map.copyOf(byNamespace);
    }

    @Override
    public Optional<SchemaBundle> find(XmlDocumentInfo info) {
        return Optional.ofNullable(byNamespace.get(info.namespaceUri()));
    }
}
