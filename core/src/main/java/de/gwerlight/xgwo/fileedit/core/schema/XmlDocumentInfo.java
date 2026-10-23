package de.gwerlight.xgwo.fileedit.core.schema;

import java.io.StringReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Grunddaten eines Dokuments, mit denen das passende Schema gefunden wird.
 *
 * @param rootLocalName Name des Wurzelelements ohne Präfix
 * @param namespaceUri  Namespace des Wurzelelements (leer, wenn keiner)
 */
public record XmlDocumentInfo(String rootLocalName, String namespaceUri) {

    /**
     * Liest nur bis zum Wurzelelement. Wirft {@link XMLStreamException}, wenn das Dokument nicht wohlgeformt ist
     * oder kein Wurzelelement enthält.
     */
    public static XmlDocumentInfo detect(String xml) throws XMLStreamException {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XMLStreamReader r = f.createXMLStreamReader(new StringReader(xml));
        try {
            while (r.hasNext()) {
                if (r.next() == XMLStreamConstants.START_ELEMENT) {
                    String ns = r.getNamespaceURI() == null ? "" : r.getNamespaceURI();
                    return new XmlDocumentInfo(r.getLocalName(), ns);
                }
            }
            throw new XMLStreamException("Kein Wurzelelement gefunden");
        } finally {
            r.close();
        }
    }
}
