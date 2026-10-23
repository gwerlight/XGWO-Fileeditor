package de.gwerlight.xgwo.fileedit.core.check;

import de.gwerlight.xgwo.fileedit.core.message.MessageTranslator;
import de.gwerlight.xgwo.fileedit.core.model.Finding;
import de.gwerlight.xgwo.fileedit.core.model.Origin;
import de.gwerlight.xgwo.fileedit.core.model.Severity;
import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** XSD-Validierung mit JAXP (nur JDK, keine Zusatzbibliothek). */
public final class XsdCheckStep implements CheckStep {

    private static final Pattern CODE = Pattern.compile("^(cvc-[A-Za-z0-9.\\-]+)");
    /** Folgefehler, die Xerces zusätzlich zur eigentlichen Ursache in derselben Zeile meldet. */
    private static final Set<String> FOLLOW_UPS = Set.of("cvc-type.3.1.3", "cvc-attribute.3");

    private final MessageTranslator translator;

    public XsdCheckStep(MessageTranslator translator) {
        this.translator = translator;
    }

    @Override
    public List<Finding> run(String xml, SchemaBundle bundle) {
        List<Finding> findings = new ArrayList<>();
        Validator validator = newValidator(bundle);
        validator.setErrorHandler(new ErrorHandler() {
            @Override public void warning(SAXParseException e) { add(Severity.WARNING, Origin.XSD, e); }
            @Override public void error(SAXParseException e) { add(Severity.ERROR, Origin.XSD, e); }
            @Override public void fatalError(SAXParseException e) throws SAXException {
                add(Severity.ERROR, Origin.WELL_FORMED, e);
                throw e;
            }

            private void add(Severity severity, Origin origin, SAXParseException e) {
                String technical = e.getMessage();
                String ruleId = null;
                if (technical != null) {
                    Matcher m = CODE.matcher(technical);
                    ruleId = m.find() ? m.group(1) : null;
                }
                if (ruleId != null && FOLLOW_UPS.contains(ruleId) && !findings.isEmpty()
                        && findings.get(findings.size() - 1).line() == e.getLineNumber()) {
                    return; // Folgefehler zur Ursache direkt davor, würde den Support nur verwirren
                }
                findings.add(new Finding(origin, severity, e.getLineNumber(), e.getColumnNumber(), null, ruleId,
                        translator.translate(origin, ruleId, technical), technical));
            }
        });
        try {
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXParseException ignored) {
            // fatalError wurde bereits als Befund erfasst
        } catch (SAXException | IOException e) {
            throw new IllegalStateException("XSD-Prüfung nicht möglich: " + e.getMessage(), e);
        }
        return findings;
    }

    private static Validator newValidator(SchemaBundle bundle) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // xs:include / xs:import nur aus lokalen Dateien bzw. Jar-Ressourcen, nie aus dem Netz
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar:file");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            // Schema bei jeder Prüfung neu laden, damit geänderte XSDs sofort wirken
            Schema schema = factory.newSchema(bundle.xsd());
            Validator v = schema.newValidator();
            v.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            v.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            v.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return v;
        } catch (SAXException e) {
            throw new IllegalStateException("Schema '" + bundle.id() + "' ist fehlerhaft: " + e.getMessage(), e);
        }
    }
}
