package de.gwerlight.xgwo.fileedit.core.schema;

import java.util.Optional;

/** Findet zu einem Dokument das passende Schema-Paket (Erkennung z. B. über Wurzelelement/Namespace). */
public interface SchemaCatalog {
    Optional<SchemaBundle> find(XmlDocumentInfo info);
}
