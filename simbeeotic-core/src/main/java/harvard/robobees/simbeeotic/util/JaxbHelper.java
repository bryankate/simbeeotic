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
