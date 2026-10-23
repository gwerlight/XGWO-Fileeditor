package de.gwerlight.xgwo.fileedit.core.service;

import de.gwerlight.xgwo.fileedit.core.model.CheckResult;
import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle;
import de.gwerlight.xgwo.fileedit.core.schema.SchemaCatalog;

/**
 * Zentrale Schnittstelle des Kerns. Desktop-UI und eine spätere Web-Anwendung rufen nur dies auf.
 * Implementierungen halten keinen UI-Zustand und sind thread-sicher.
 */
public interface XmlCheckService {

    /** Erkennt das passende Schema automatisch (über {@link SchemaCatalog}) und prüft das Dokument. */
    CheckResult check(String xml);

    /** Prüft gegen ein bewusst gewähltes Schema-Paket (z. B. für Tests oder wenn die Erkennung versagt). */
    CheckResult check(String xml, SchemaBundle bundle);
}
