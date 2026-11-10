package io.github.dqueir0z.erp.nfe;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

/**
 * Leitura de XML com as proteções contra XXE ligadas: o XML vem do banco e pode ter
 * sido alterado, então DOCTYPE, entidades externas e XInclude são recusados.
 */
final class XmlSeguro {

    private XmlSeguro() {
    }

    static Document ler(String xml, String descricao) throws XmlNfeInvalidoException {
        try {
            DocumentBuilder builder = fabrica().newDocumentBuilder();
            // Sem isto o parser imprime "[Fatal Error]" no console antes de lançar a exceção.
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException | IOException e) {
            throw new XmlNfeInvalidoException(descricao + " não é um XML válido: " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Parser XML sem suporte às proteções exigidas.", e);
        }
    }

    private static DocumentBuilderFactory fabrica() throws ParserConfigurationException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
        fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        fabrica.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);
        return fabrica;
    }
}
