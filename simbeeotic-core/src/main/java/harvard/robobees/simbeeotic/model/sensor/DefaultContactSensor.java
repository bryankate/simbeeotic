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
package harvard.robobees.simbeeotic.model.sensor;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.model.Contact;
import harvard.robobees.simbeeotic.model.Model;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.CollisionEvent;
import harvard.robobees.simbeeotic.model.EventHandler;
import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;
import java.util.Set;
import java.util.HashSet;


/**
 * @author bkate
 */
public class DefaultContactSensor extends AbstractSensor implements ContactSensor {

    private float radius = 0.005f;  // m

    private Set<ContactSensorListener> listeners = new HashSet<ContactSensorListener>();


    /** {@inheritDoc} */
    public boolean isTripped() {

        // try each known contact point and determine if it is within the sensor's area.
        for (Contact contact : getHost().getContactPoints()) {

            Vector3f diff = new Vector3f(contact.getBodyContactPoint());

            diff.sub(getOffset());

            if (diff.length() <= radius) {
                return true;
            }
        }

        return false;
    }


    /**
     * Handles events that are generated when collisions occur on the parent model.
     *
     * @param time The time of the collision.
     * @param event The corresponding event.
     */
    @EventHandler
    public final void handleCollisionEvent(SimTime time, CollisionEvent event) {

        // just because we had a collision on the parent, it doesn't mean
        // it occurred in the range of the sensor
        if (isTripped()) {

            for (ContactSensorListener listener : listeners) {
                listener.tripped(time, this);
            }
        }
    }


    /**
     * {@inheritDoc}
     *
     * This imeplmentation adds the sensor as a listener for {@link CollisionEvent}s.
     */
    @Override
    public void setParentModel(Model parent) {
        
        super.setParentModel(parent);

        ((PhysicalEntity)parent).addCollisionListener(getModelId());
    }


    /** {@inheritDoc} */
    public final void addListener(ContactSensorListener listener) {
        listeners.add(listener);
    }


    /** {@inheritDoc} */
    public final void removeListener(ContactSensorListener listener) {
        listeners.remove(listener);
    }


    @Inject(optional = true)
    public final void setradius(@Named("radius") final float radius) {
        this.radius = radius;
    }
}
