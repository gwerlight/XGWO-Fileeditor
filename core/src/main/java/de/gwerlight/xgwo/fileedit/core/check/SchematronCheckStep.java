package de.gwerlight.xgwo.fileedit.core.check;

import de.gwerlight.xgwo.fileedit.core.model.Finding;
import de.gwerlight.xgwo.fileedit.core.model.Origin;
import de.gwerlight.xgwo.fileedit.core.model.Severity;
import de.gwerlight.xgwo.fileedit.core.schema.SchemaBundle;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltExecutable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Fachliche Prüfung mit vorkompilierten Schematron-Regeln (XSLT, Ergebnis im SVRL-Format), ausgeführt mit Saxon-HE.
 * Die XSLT-Dateien stammen aus {@link SchemaBundle#schematron()}. Zeile und Spalte eines Befunds werden über den
 * XPath-Ort aus dem SVRL im Originaldokument ermittelt.
 */
public final class SchematronCheckStep implements CheckStep {

    private static final String SVRL_NS = "http://purl.oclc.org/dsdl/svrl";

    /**
     * Kompiliertes Stylesheet samt seinen Namespace-Präfixen; die große XSLT soll nicht bei jeder Prüfung neu übersetzt
     * werden. Die Präfixe werden gebraucht, weil der Ort im SVRL (z. B. {@code /xga:satz/...}) mit ihnen geschrieben ist.
     */
    private record Compiled(long lastModified, XsltExecutable executable, Map<String, String> namespaces) { }

    private final Processor processor = new Processor(false);
    private final ConcurrentMap<String, Compiled> cache = new ConcurrentHashMap<>();

    @Override
    public List<Finding> run(String xml, SchemaBundle bundle) {
        if (bundle.schematron().isEmpty()) {
            return List.of();
        }
        List<Finding> findings = new ArrayList<>();
        try {
            DocumentBuilder builder = processor.newDocumentBuilder();
            builder.setLineNumbering(true);
            XdmNode document = builder.build(secureSource(xml));
            for (URL xsl : bundle.schematron()) {
                try {
                    runStylesheet(xsl, document, findings);
                } catch (SaxonApiException | IOException e) {
                    findings.add(systemFinding("Die fachliche Prüfung '" + fileName(xsl)
                            + "' konnte nicht ausgeführt werden: " + e.getMessage()));
                }
            }
        } catch (SaxonApiException | SAXException | ParserConfigurationException e) {
            throw new IllegalStateException("Schematron-Prüfung nicht möglich: " + e.getMessage(), e);
        }
        return findings;
    }

    private void runStylesheet(URL xsl, XdmNode document, List<Finding> findings) throws SaxonApiException, IOException {
        XdmDestination svrl = new XdmDestination();
        Compiled compiled = compiled(xsl);
        compiled.executable().load30().applyTemplates(document, svrl);
        XPathCompiler xpath = processor.newXPathCompiler();
        xpath.declareNamespace("svrl", SVRL_NS);
        XPathSelector results = xpath.compile("//svrl:failed-assert | //svrl:successful-report").load();
        results.setContextItem(svrl.getXdmNode());
        XPathSelector text = xpath.compile("string(svrl:text[1])").load();
        for (XdmItem item : results.evaluate()) {
            XdmNode node = (XdmNode) item;
            text.setContextItem(node);
            String location = node.getAttributeValue(new QName("location"));
            String message = text.evaluateSingle().getStringValue().strip();
            String test = node.getAttributeValue(new QName("test"));
            int[] position = positionOf(location, document, compiled.namespaces());
            findings.add(new Finding(Origin.SCHEMATRON, severityOf(node.getAttributeValue(new QName("role"))),
                    position[0], position[1], location, node.getAttributeValue(new QName("id")),
                    message, "Regel nicht erfüllt: " + test));
        }
    }

    /** Ermittelt Zeile/Spalte des Elements, auf das sich der XPath-Ort bezieht; {@code -1}, wenn nicht bestimmbar. */
    private int[] positionOf(String location, XdmNode document, Map<String, String> namespaces) {
        if (location != null && !location.isBlank()) {
            try {
                XPathCompiler locator = processor.newXPathCompiler();
                namespaces.forEach(locator::declareNamespace);
                XdmItem hit = locator.evaluateSingle(location, document);
                if (hit instanceof XdmNode node) {
                    return new int[] {node.getLineNumber(), node.getColumnNumber()};
                }
            } catch (SaxonApiException ignored) {
                // Ort nicht auflösbar: der Befund bleibt ohne Position erhalten
            }
        }
        return new int[] {-1, -1};
    }

    private static Severity severityOf(String role) {
        if (role == null) {
            return Severity.ERROR;
        }
        return switch (role.toLowerCase(Locale.ROOT)) {
            case "warning", "warn" -> Severity.WARNING;
            case "info", "information" -> Severity.INFO;
            default -> Severity.ERROR;
        };
    }

    private Compiled compiled(URL xsl) throws SaxonApiException, IOException {
        long stamp = lastModified(xsl);
        Compiled cached = cache.get(xsl.toExternalForm());
        if (cached != null && cached.lastModified() == stamp) {
            return cached;
        }
        XsltExecutable executable = processor.newXsltCompiler().compile(new StreamSource(xsl.toExternalForm()));
        Compiled compiled = new Compiled(stamp, executable, namespacesOf(xsl));
        cache.put(xsl.toExternalForm(), compiled);
        return compiled;
    }

    /** Liest die Namespace-Präfixe vom Wurzelelement des Stylesheets. */
    private Map<String, String> namespacesOf(URL xsl) throws SaxonApiException {
        XdmNode document = processor.newDocumentBuilder().build(new StreamSource(xsl.toExternalForm()));
        XdmNode root = null;
        // Vor dem Wurzelelement dürfen Kommentare oder Anweisungen stehen, daher nicht einfach das erste Kind nehmen
        for (XdmNode child : document.children()) {
            if (child.getNodeKind() == net.sf.saxon.s9api.XdmNodeKind.ELEMENT) {
                root = child;
                break;
            }
        }
        Map<String, String> namespaces = new HashMap<>();
        if (root == null) {
            return namespaces;
        }
        var iterator = root.axisIterator(Axis.NAMESPACE);
        while (iterator.hasNext()) {
            XdmNode ns = iterator.next();
            if (ns.getNodeName() != null && !ns.getNodeName().getLocalName().isEmpty()) {
                namespaces.put(ns.getNodeName().getLocalName(), ns.getStringValue());
            }
        }
        return namespaces;
    }

    /** Änderungszeit, damit eine bearbeitete externe XSLT ohne Neustart wirkt. */
    private static long lastModified(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        try {
            return connection.getLastModified();
        } finally {
            connection.getInputStream().close();
        }
    }

    /** XML ohne Nachladen externer Entitäten/DTDs. */
    private static SAXSource secureSource(String xml) throws SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        XMLReader reader = factory.newSAXParser().getXMLReader();
        return new SAXSource(reader, new InputSource(new StringReader(xml)));
    }

    private static Finding systemFinding(String message) {
        return new Finding(Origin.SYSTEM, Severity.WARNING, -1, -1, null, null, message, message);
    }

    private static String fileName(URL url) {
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
