/*
 * Copyright (c) 2012, The President and Fellows of Harvard College.
 * All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package harvard.robobees.simbeeotic.util;


import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Convenience methods for orking with XML documents.
 *
 * @author bkate
 */
public class DocUtil {

    private static final Logger logger = Logger.getLogger(DocUtil.class);

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
     * @return True if the value matches the {@link DocUtil#PLACEHOLDER_PATTERN} regular expression.
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
     * Sets the namespace URI of the given document. Any non-w3c namespace URIs
     * for elements and attributes will be set to the given namespace URI.
     *
     * @param doc The document to modify.
     * @param newNS The new namespace URI.
     *
     * @return The document, modified to reflect the new namespace URI.
     */
    public static Document setDocumentNamespace(final Document doc, final String newNS) {

        Stack<Node> nodes = new Stack<Node>();

        nodes.push(doc.getDocumentElement());

        while (!nodes.isEmpty()) {

            Node node = nodes.pop();

            switch (node.getNodeType()) {

                case Node.ATTRIBUTE_NODE:
                case Node.ELEMENT_NODE:

                    if (node.getNamespaceURI() == null) {
                        break;
                    }

                    // skip over XSD namespace
                    if (node.getNamespaceURI().contains("www.w3.org")) {
                        break;
                    }

                    // the reassignment to node is very important. as per javadoc renameNode will
                    // try to modify node (first parameter) in place. If that is not possible it
                    // will replace that node for a new created one and return it to the caller.
                    // if we did not reassign node we will get no childs in the loop below.
                    node = doc.renameNode(node, newNS, node.getNodeName());
                    break;
            }

            // for attributes of this node
            NamedNodeMap attributes = node.getAttributes();

            if (!(attributes == null || attributes.getLength() == 0)) {

                for (int i = 0, count = attributes.getLength(); i < count; ++i) {

                    Node attribute = attributes.item(i);

                    if (attribute != null) {
                        nodes.push(attribute);
                    }
                }
            }

            // for child nodes of this node
            NodeList childNodes = node.getChildNodes();

            if (!(childNodes == null || childNodes.getLength() == 0)) {

                for (int i = 0, count = childNodes.getLength(); i < count; ++i) {

                    Node childNode = childNodes.item(i);

                    if (childNode != null) {
                        nodes.push(childNode);
                    }
                }
            }
        }

        doc.normalizeDocument();

        return doc;
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
