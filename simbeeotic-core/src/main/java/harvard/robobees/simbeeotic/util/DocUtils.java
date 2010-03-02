package harvard.robobees.simbeeotic.util;


import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author bkate
 */
public class DocUtils {

    private static final Logger logger = Logger.getLogger(DocUtils.class);

    /**
     * regex for variable placeholders.<p/>
     *
     * variable placeholders can have the following pattern: ${name}:default
     * where the name begins with an alpha character, and has optional alternating sections of alphanumeric characters
     * separated by a '-', '_', or '.', and ends with an alphanumeric section. in addition, a default value can be
     * specified by placing a colon after the variable, followed by the default value, which is unrestricted.
     * whitespace before and after the variable is ignored (unless the variable has a default, in which case the
     * whitespace at the end is considered part of the default value).<p/>
     *
     * examples: ${a}, ${foo}, ${foo.bar}, ${f.o.0.bar-baz_9}, and ${foo}:800 are valid placeholders,
     * while ${0}, ${foo..bar}, and ${foo}: are invalid.
     */
    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\s*((?:\\$\\{([a-zA-Z][a-zA-Z0-9]*(?:[-_\\.](?:[a-zA-Z0-9])+)*)\\})(?:\\:(.+))?)\\s*");


    /**
     * Determines if a string represents a document variable placeholder.
     *
     * @param value The value to check.
     *
     * @return True if the value matches the {@link DocUtils#PLACEHOLDER_PATTERN} regular expression.
     */
    public static boolean isPlaceholder(String value) {

        if (value != null) {
            return PLACEHOLDER_PATTERN.matcher(value).matches();
        }

        return false;
    }


    /**
     * Gets the name of the placeholder variable from a placeholder value.
     *
     * @param placeholder The value that is determined to be a placeholder.
     *
     * @return The name of the variable, or null if the value does not match.
     */
    public static String extractPlaceholderName(String placeholder) {

        if (placeholder != null) {

            Matcher matcher = PLACEHOLDER_PATTERN.matcher(placeholder);

            matcher.find();
            return matcher.group(2);
        }

        return null;
    }


    /**
     * Gets the default value of a placeholder variable.
     *
     * @param placeholder The value that is determined to be a placeholder.
     *
     * @return The default value, is one is specified in the placeholder - null otherwise.
     */
    public static String extractPlaceholderDefault(String placeholder) {

        if (placeholder != null) {

            Matcher matcher = PLACEHOLDER_PATTERN.matcher(placeholder);

            matcher.find();
            return matcher.group(3);
        }

        return null;
    }


    /**
     * Creates a new empty docuemnt.
     *
     * @return A new DOM document.  Will return <code>null</code> if an exception occurs during document contruction.
     */
    public static Document createDocument() {

        DocumentBuilder builder;

        try {

            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            builder = bf.newDocumentBuilder();

            return builder.newDocument();
        }
        catch (ParserConfigurationException e) {
            logger.error("Unable to create DocumentBuilder", e);
        }

        return null;
    }


    /**
     * Creates an exact clone of a Document.
     *
     * @param doc The document being cloned.
     *
     * @return A Document with the same Element tree, or null if null is passed in.
     */
    public static Document cloneDocument(final Document doc) {

        if (doc == null) {
            throw new IllegalArgumentException("The document source cannot be null.");
        }

        // for now do this, but it may need to be changed later if this is deemed implemenation dependent
        return (Document)doc.cloneNode(true);
    }


    /**
     * Creates a new Document with the specified element serving as the document root.
     *
     * @param documentRoot The root element of the Document being created.
     * @return A new DOM document.  Will return <code>null</code> if an exception occurs during document contruction.
     */
    public static Document createDocumentFromElement(final Element documentRoot) {

        if (documentRoot == null) {
            throw new IllegalArgumentException("The element source cannot be null.");
        }

        DocumentBuilder builder;

        try {

            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            builder = bf.newDocumentBuilder();

            Document ret = builder.newDocument();
            ret.appendChild(ret.importNode(documentRoot, true));

            return ret;
        }
        catch (ParserConfigurationException e) {
            logger.error("Unable to create DocumentBuilder", e);
        }

        return null;
    }


    /**
     * This method parses an XML document from an InputStream and returns a Document object view of the
     * XML document.
     *
     * @param xmlStream A stream into a well formed XML document.
     *
     * @return A Document object that represents the XML document, or <code>null</code> if an exception occurs while
     *         creating the document.
     */
    public static Document getDocumentFromXml(final InputStream xmlStream) {

        if (xmlStream == null) {
            throw new IllegalArgumentException("The xml input stream cannot be null.");
        }

        try {

            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);

            DocumentBuilder builder = bf.newDocumentBuilder();

            return builder.parse(xmlStream);
        }
        catch (ParserConfigurationException e) {
            logger.error("Unable to instantiate XML parser", e);
        }
        catch (SAXException e) {
            logger.error("Could not parse XML document", e);
        }
        catch (IOException e) {
            logger.error("Could not read XML docuemnt", e);
        }

        return null;
    }

    /**
     * This method parses an XML document from an InputStream and returns a Document object view of the
     * XML document.
     *
     * @param xmlString A string of well formed XML document.
     *
     * @return A Document object that represents the XML document, or <code>null</code> if an exception occurs while
     *         creating the document.
     */
    public static Document getDocumentFromXml(final String xmlString) {

        if (xmlString == null) {
            throw new IllegalArgumentException("The xml string cannot be null.");
        }

        try {

            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);

            DocumentBuilder builder = bf.newDocumentBuilder();

            return builder.parse(new InputSource(new StringReader(xmlString)));
        }
        catch (ParserConfigurationException e) {
            logger.error("Unable to instantiate XML parser", e);
        }
        catch (SAXException e) {
            logger.error("Could not parse XML document", e);
        }
        catch (IOException e) {
            logger.error("Could not read XML docuemnt", e);
        }

        return null;
    }

}
