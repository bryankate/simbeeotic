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
package harvard.robobees.simbeeotic.model;


import javax.vecmath.Vector3f;
import java.util.Map;


/**
 * A container class for information related to a contact between two
 * physical objects in the virtual world.
 * 
 * @author bkate
 */
public class Contact {

    private Vector3f bodyContactPoint;
    private Vector3f worldContactPoint;

    // the properties of the object we are touching
    private Map<String, Object> contactMetadata;


    public Contact(final Vector3f body, final Vector3f world, final Map<String, Object> props) {

        this.bodyContactPoint = new Vector3f(body);
        this.worldContactPoint = new Vector3f(world);
        this.contactMetadata = props;
    }


    /**
     * Gets the point of contact in body coordinates.
     *
     * @return The point on the body at which contact is made.
     */
    public Vector3f getBodyContactPoint() {
        return bodyContactPoint;
    }


    /**
     * Gets the point of contact in world coordinates.
     *
     * @return The point in the world in which contact is made.
     */
    public Vector3f getWorldContactPoint() {
        return worldContactPoint;
    }


    /**
     * Gets the properties of the object that is being touched.
     *
     * @return The object's user-defined proeprties.
     */
    public Map<String, Object> getContactMetadata() {
        return contactMetadata;
    }


    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Contact contact = (Contact) o;

        if (!bodyContactPoint.equals(contact.bodyContactPoint)) {
            return false;
        }

        if (!worldContactPoint.equals(contact.worldContactPoint)) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {

        int result = bodyContactPoint.hashCode();
        result = 31 * result + worldContactPoint.hashCode();

        return result;
    }
}
