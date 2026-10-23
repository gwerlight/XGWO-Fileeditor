package de.gwerlight.xgwo.fileedit.desktop.spec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Prüfungen für eine einzurichtende Spezifikation, bevor sie verwendet wird. */
public final class SpecificationChecks {

    private static final String XSD_NS = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    private static final String XSLT_NS = "http://www.w3.org/1999/XSL/Transform";

    // Reine Zahl mit Punkten, z. B. 1.6 oder 2
    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+)*");

    private SpecificationChecks() { }

    /**
     * Schnelle Prüfung ohne das Schema zu laden: Version, Dateien vorhanden und vom richtigen Typ, und ob XSD und
     * XSL zur Version passen (die Version muss im Namespace der XSD und in einem Namespace der XSL vorkommen).
     *
     * @return verständliche Probleme, leer wenn alles passt
     */
    public static List<String> problems(String version, Path xsd, Path xsl) {
        List<String> problems = new ArrayList<>();
        String number = version == null ? "" : version.strip();
        boolean versionOk = VERSION.matcher(number).matches();
        if (!versionOk) {
            problems.add("Die Version fehlt oder ist keine Zahl (z. B. 1.6).");
        }
        if (xsd == null || !Files.isRegularFile(xsd)) {
            problems.add("Die XSD-Datei fehlt oder existiert nicht.");
        } else if (!hasRoot(xsd, XSD_NS, Set.of("schema"))) {
            problems.add("Die XSD-Datei ist keine XML-Schema-Datei.");
        } else {
            Optional<String> namespace = targetNamespace(xsd).filter(ns -> !ns.isEmpty());
            if (namespace.isEmpty()) {
                problems.add("Die XSD hat keinen targetNamespace, daher kann das Schema nicht automatisch erkannt werden.");
            } else if (versionOk && !containsVersion(namespace.get(), number)) {
                problems.add("Die Version " + number + " kommt im Namespace der XSD nicht vor (" + namespace.get()
                        + "). Passt die XSD zur Version?");
            }
        }
        if (xsl == null || !Files.isRegularFile(xsl)) {
            problems.add("Die Schematron-XSL fehlt oder existiert nicht (sie gehört zwingend zur Spezifikation).");
        } else if (!hasRoot(xsl, XSLT_NS, Set.of("stylesheet", "transform"))) {
            problems.add("Die Schematron-XSL ist kein XSLT-Stylesheet.");
        } else if (versionOk && declaredNamespaces(xsl).stream().noneMatch(ns -> containsVersion(ns, number))) {
            problems.add("Die Schematron-XSL deklariert keinen Namespace mit der Version " + number
                    + ". Gehört sie zu dieser Spezifikation und Version?");
        }
        return problems;
    }

    /**
     * Name der Spezifikation: der Kurzname aus der XSD ({@code nameKurz} im Abschnitt {@code standard}), sonst der
     * Dateiname der XSD ohne Endung.
     */
    public static String specificationName(Path xsd) {
        return appinfo(xsd).orElseGet(() -> {
            String file = xsd.getFileName().toString();
            int dot = file.lastIndexOf('.');
            return dot > 0 ? file.substring(0, dot) : file;
        });
    }

    /** Namespace, für den die XSD gilt ({@code targetNamespace}); leer, wenn die Datei nicht lesbar ist. */
    public static Optional<String> targetNamespace(Path xsd) {
        return rootElement(xsd, r -> Optional.of(r.getAttributeValue(null, "targetNamespace") == null
                ? "" : r.getAttributeValue(null, "targetNamespace")));
    }

    /**
     * Lädt die XSD tatsächlich (samt Includes). Das dauert bei großen Schemas etwas und gehört deshalb nicht in die
     * schnelle Prüfung.
     *
     * @return die Fehlermeldung des Schema-Parsers, leer wenn die XSD ladbar ist
     */
    public static Optional<String> loadError(Path xsd) {
        // Eine nicht auffindbare Include-Datei meldet der Parser nur als Warnung und lädt das Schema trotzdem unvollständig
        List<String> messages = new ArrayList<>();
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XSD_NS);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar:file");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setErrorHandler(new ErrorHandler() {
                @Override public void warning(SAXParseException e) { messages.add(e.getMessage()); }
                @Override public void error(SAXParseException e) { messages.add(e.getMessage()); }
                @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
            });
            factory.newSchema(new StreamSource(xsd.toUri().toString()));
        } catch (SAXException e) {
            return Optional.of(e.getMessage());
        }
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }

    /** Kommt die Versionsnummer als eigenständige Zahl im Namespace vor (z. B. {@code .../xga/1.6} oder {@code ..._1_6})? */
    static boolean containsVersion(String namespace, String version) {
        // Punkte der Version dürfen im Namespace auch als Unterstrich geschrieben sein
        String number = Pattern.quote(version).replace(".", "\\E[._]\\Q");
        return Pattern.compile("(?<![0-9])" + number + "(?![0-9]|[._][0-9])").matcher(namespace).find();
    }

    /** Namespaces, die das Wurzelelement der Datei deklariert. */
    private static List<String> declaredNamespaces(Path file) {
        return rootElement(file, r -> {
            List<String> uris = new ArrayList<>();
            for (int i = 0; i < r.getNamespaceCount(); i++) {
                uris.add(r.getNamespaceURI(i));
            }
            return Optional.of(uris);
        }).orElse(List.of());
    }

    /**
     * Liest einen Wert aus dem Beschreibungsblock am Anfang der XSD:
     * {@code schema/annotation/appinfo/<group>/<element>}. Es wird nur bis zum Ende der ersten Annotation gelesen.
     */
    private static Optional<String> appinfo(Path xsd) {
        try (InputStream in = Files.newInputStream(xsd)) {
            XMLStreamReader r = openReader(in);
            try {
                List<String> path = new ArrayList<>();
                while (r.hasNext()) {
                    int event = r.next();
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        path.add(r.getLocalName());
                        if (path.size() == 2 && !"annotation".equals(path.get(1))) {
                            return Optional.empty();
                        }
                        if (path.size() == 5 && "appinfo".equals(path.get(2)) && "standard".equals(path.get(3))
                                && "nameKurz".equals(path.get(4))) {
                            String text = r.getElementText().strip();
                            return text.isEmpty() ? Optional.empty() : Optional.of(text);
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        path.remove(path.size() - 1);
                        if (path.size() == 1) {
                            return Optional.empty();
                        }
                    }
                }
            } finally {
                r.close();
            }
        } catch (IOException | XMLStreamException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /** Reader ohne DTD und externe Entities; Fabrik und Reader entstehen hier gemeinsam, damit die Härtung eindeutig ist. */
    private static XMLStreamReader openReader(InputStream in) throws XMLStreamException {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return f.createXMLStreamReader(in);
    }

    private static boolean hasRoot(Path file, String namespace, Set<String> localNames) {
        Optional<Boolean> matches = rootElement(file, r -> {
            boolean namespaceMatches = namespace.equals(r.getNamespaceURI());
            boolean nameMatches = localNames.contains(r.getLocalName());
            return namespaceMatches && nameMatches ? Optional.of(Boolean.TRUE) : Optional.<Boolean>empty();
        });
        return matches.isPresent();
    }

    /** Wertet das Wurzelelement aus; leer, wenn die Datei nicht lesbar ist oder kein Wurzelelement hat. */
    private static <T> Optional<T> rootElement(Path file, Function<XMLStreamReader, Optional<T>> reader) {
        try (InputStream in = Files.newInputStream(file)) {
            XMLStreamReader r = openReader(in);
            try {
                while (r.hasNext()) {
                    if (r.next() == XMLStreamConstants.START_ELEMENT) {
                        return reader.apply(r);
                    }
                }
            } finally {
                r.close();
            }
        } catch (IOException | XMLStreamException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
