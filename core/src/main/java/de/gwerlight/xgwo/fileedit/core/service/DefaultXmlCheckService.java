package de.gwerlight.xgwo.fileedit.core.service;

import de.gwerlight.xgwo.fileedit.core.check.CheckStep;
import de.gwerlight.xgwo.fileedit.core.check.SchematronCheckStep;
import de.gwerlight.xgwo.fileedit.core.check.XsdCheckStep;
import de.gwerlight.xgwo.fileedit.core.message.DefaultMessageTranslator;
import de.gwerlight.xgwo.fileedit.core.message.MessageTranslator;
import de.gwerlight.xgwo.fileedit.core.model.CheckResult;
import de.gwerlight.xgwo.fileedit.core.model.Finding;
import de.gwerlight.xgwo.fileedit.core.model.Origin;
import de.gwerlight.xgwo.fileedit.core.model.Severity;
import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle;
import de.gwerlight.xgwo.fileedit.core.schema.SchemaCatalog;
import de.gwerlight.xgwo.fileedit.core.schema.XmlDocumentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

public final class DefaultXmlCheckService implements XmlCheckService {

    private final SchemaCatalog catalog;
    private final List<CheckStep> steps;
    private final MessageTranslator translator;

    public DefaultXmlCheckService(SchemaCatalog catalog, List<CheckStep> steps, MessageTranslator translator) {
        this.catalog = catalog;
        this.steps = List.copyOf(steps);
        this.translator = translator;
    }

    /** Standardaufbau: XSD- und Schematron-Prüfung. Weitere Schritte (z. B. Mapping-Regeln) einfach in die Liste aufnehmen. */
    public static DefaultXmlCheckService withDefaults(SchemaCatalog catalog) {
        MessageTranslator translator = new DefaultMessageTranslator();
        return new DefaultXmlCheckService(catalog, List.of(new XsdCheckStep(translator), new SchematronCheckStep()), translator);
    }

    @Override
    public CheckResult check(String xml) {
        XmlDocumentInfo info;
        try {
            info = XmlDocumentInfo.detect(xml);
        } catch (XMLStreamException e) {
            return new CheckResult(null, List.of(wellFormedFinding(e)));
        }
        Optional<SchemaBundle> bundle = catalog.find(info);
        if (bundle.isEmpty()) {
            String msg = "Für das Wurzelelement '" + info.rootLocalName() + "' ist kein Schema hinterlegt.";
            return new CheckResult(null, List.of(
                    new Finding(Origin.SYSTEM, Severity.ERROR, 1, 1, null, null, msg, msg)));
        }
        return check(xml, bundle.get());
    }

    @Override
    public CheckResult check(String xml, SchemaBundle bundle) {
        List<Finding> all = new ArrayList<>();
        for (CheckStep step : steps) {
            all.addAll(step.run(xml, bundle));
            // Bei nicht wohlgeformtem XML sind weitere Schritte sinnlos
            if (all.stream().anyMatch(f -> f.origin() == Origin.WELL_FORMED)) {
                break;
            }
        }
        return new CheckResult(bundle.id(), all);
    }

    private Finding wellFormedFinding(XMLStreamException e) {
        Location loc = e.getLocation();
        int line = loc == null ? -1 : loc.getLineNumber();
        int col = loc == null ? -1 : loc.getColumnNumber();
        String technical = e.getMessage();
        return new Finding(Origin.WELL_FORMED, Severity.ERROR, line, col, null, null,
                translator.translate(Origin.WELL_FORMED, null, technical), technical);
    }
}
