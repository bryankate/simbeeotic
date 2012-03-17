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


import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;


/**
 * A class that acts as a go-between for Simbeeotic models and the JBullet bodies
 * that represent them in the physics engine. An instance of this class is attached
 * to each JBullet body, so when an event occurs in the physics engine (like a
 * collision) we can make updates to it that will be seen by the Simbeeotic models.
 * This way we do not need to keep a global map associating bodies with models.
 *
 * @author bkate
 */
public class EntityInfo {

    private int objectId;
    private Map<String, Object> metadata = new HashMap<String, Object>();
    private Set<Contact> contactPoints = new HashSet<Contact>();
    private Set<Integer> collisionListeners = new HashSet<Integer>();


    public EntityInfo(final int objectId) {

        this.objectId = objectId;
        this.metadata = new HashMap<String, Object>();
    }


    public EntityInfo(final int objectId, final Map<String, Object> meta) {

        this.objectId = objectId;
        this.metadata = meta;
    }


    /**
     * Gets the object identifier for the physical object being represented.
     *
     * @return The object identifier (not to be confused with the model ID).
     */
    public int getObjectId() {
        return objectId;
    }


    /**
     * Get the current set of contact points for the physical body.
     *
     * @return The current set of contact points, or an empty set if none exist.
     */
    public Set<Contact> getContactPoints() {
        return contactPoints;
    }


    /**
     * Gets the set of {@link Model}s that should receive a {@link CollisionEvent}
     * when a collision is detected between the physical body and another body.
     *
     * <br/>
     * This is a bit of a hack, but it is better than broadcasting an event
     * when a collision occurs and having every model check if it is involved.
     *
     * @return The model identifiers of the interested models.
     */
    public Set<Integer> getCollisionListeners() {
        return collisionListeners;
    }


    /**
     * Gets the metadata associated with the physical body.
     *
     * @return The metadata for this physical body.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
