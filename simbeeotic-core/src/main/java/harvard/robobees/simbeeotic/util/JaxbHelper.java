package harvard.robobees.simbeeotic.util;


import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;


/**
 * @author bkate
 */
public class JaxbHelper {

    /**
     * Returns an object structure that represents the node instance passed in.
     *
     * @param node The document instance that is to be unmarshalled.
     * @param type The type of object that is to be created from the node.
     *
     * @return An instance of 'type' that represents the 'node' instance.
     *
     * @throws javax.xml.bind.JAXBException Thrown if there is an error in JAXB while converting the node.
     * @throws RuntimeValidationException Thrown if there is an error validating the node.
     */
    public static <T> T objectFromNode(Node node, Class<T> type) throws JAXBException, RuntimeValidationException {

        // try to parse the XML into an object structure
        JAXBContext context = JAXBContext.newInstance(type.getPackage().getName());
        Unmarshaller u = context.createUnmarshaller();

        // set and event handler that will determine if any validation erros are fatal or not
        u.setEventHandler(new ValidationEventHandlerImpl());

        JAXBElement<?> element = u.unmarshal(node, type);

        return (T)element.getValue();
    }


    /**
     * A class that can handle validation errors and throw the appropriate excpetion.
     */
    private static class ValidationEventHandlerImpl implements ValidationEventHandler {

        public boolean handleEvent(ValidationEvent ve) {

            if ((ve.getSeverity() == ValidationEvent.ERROR) || (ve.getSeverity() == ValidationEvent.FATAL_ERROR)) {

                int line = ve.getLocator().getLineNumber();

                // fail for any fatal error
                throw new RuntimeValidationException("The document is invalid at line: " +
                                                     line + "\n" + ve.getMessage());
            }

            return false;
        }
    }
}
