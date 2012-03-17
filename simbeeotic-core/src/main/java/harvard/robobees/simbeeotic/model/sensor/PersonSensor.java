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


import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.Contact;
import harvard.robobees.simbeeotic.model.EntityInfo;
import harvard.robobees.simbeeotic.environment.WorldMap;
import harvard.robobees.simbeeotic.environment.WorldObject;

import javax.vecmath.Vector3f;
import java.util.Set;
import java.util.HashSet;


/**
 * A sensor that can detect if there are people within range of a sensor that is pointing
 * away from the body into the environment. The sensor's coverage volume is modeled as a
 * cone, and some approximations are made when determining if flowers intersect this volume.
 *
 * @author chartier
 */
public class PersonSensor extends AbstractSensor {

    private DiscreteDynamicsWorld world;
    private WorldMap map;

    private float maxRange  = 1.0f;                // m;
    private float halfAngle = (float)Math.PI / 8;  // rad

    private static final float CONTACT_EPSILON = 0.01f;


    /**
     * Determines if there is a person object within range and field of view of the sensor.
     *
     * @return True if a person is detected, false otherwise.
     */
    public boolean isPersonDetected() {
        return !sensePeople().isEmpty();
    }


    /**
     * Determines if there is a person object within range and field of view of the sensor.
     *
     * @return The meta-properties of the person detected.
     */
    public Set<WorldObject> detectPeople() {
        return sensePeople();
    }


    private Set<WorldObject> sensePeople() {

        // check if the sensor is actually in contact with another object.
        // if there is an object contacting the body in the vicinity of the sensor,
        // JBullet will actually see through the adjacent object to the next object. for example,
        // consider the body resting on a box that is sitting on the ground. if the sensor
        // is on the bottom of the body (which is contacting the box), the ray cast downward
        // will see through the box and hit the floor, even though the range to the closest
        // object should be 0. perhaps this is not the best strategy, but it works for most cases.
        for (Contact c : getHost().getContactPoints()) {

            Vector3f diff = new Vector3f();
            diff.sub(getOffset(), c.getBodyContactPoint());

            if (diff.length() <= CONTACT_EPSILON) {
                return new HashSet<WorldObject>();
            }
        }

        // we need to find the sensor's position and pointing vector
        // (in world coordinates) given the body's current orientation
        Vector3f rotatedOffset = new Vector3f(getOffset());
        Vector3f rotatedPointing = new Vector3f(getPointing());

        rotatedPointing.scale(maxRange);

        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(getHost().getTruthOrientation());

        trans.transform(rotatedOffset);
        trans.transform(rotatedPointing);

        Vector3f from = new Vector3f(getHost().getTruthPosition());
        Vector3f to = new Vector3f(getHost().getTruthPosition());

        from.add(rotatedOffset);
        to.add(rotatedOffset);
        to.add(rotatedPointing);

        Set<WorldObject> conePeople = map.getPeople(from, to, maxRange, halfAngle);

		// for every WorldObject representing a person in the 'field of view' cone,
        // attempt to trace a path using a ray; if an obstacle is encountered prematurely,
        // remove the WorldObject from the set
        
        for(WorldObject person: conePeople) {
        	// Initialize ray
    		RayCallback callback = new RayCallback(person.getObjectId(), maxRange);
        	
        	to = person.getTruthPosition();
        	
			// Send ray
			world.rayTest(from, to, callback);
        	
			if(!callback.isPersonDetected())
				conePeople.remove(person);
        }
        
        return conePeople;
    }


    @Inject
    public final void setDynamicsWorld(@GlobalScope DiscreteDynamicsWorld world) {
        this.world = world;
    }


    @Inject
    public final void setWorldMap(@GlobalScope WorldMap map) {
        this.map = map;
    }


    @Inject(optional = true)
    public final void setMaxRange(@Named("max-range") final float maxRange) {
        this.maxRange = maxRange;
    }


    /**
     * Sets the half-angle of the sensor. If the sensor is viewed as a cone, this would be
     * the angle from the center of the cone to the outer edge.
     *
     * @param angle The half-angle of the sensor's field of view.
     */
    @Inject(optional = true)
    public final void setAngle(@Named("half-angle") final float angle) {
        this.halfAngle = angle;
    }


    /**
     * A callback that handles ray intersections with objects in the world. It records
     * the minimum distance to any object.
     */
    private static class RayCallback extends CollisionWorld.RayResultCallback {

    	private int personObjectId;
        private float rayLength;

        private boolean objectDetected = false;

        private boolean personDetected = false;
        
        public RayCallback(int personId, float length) {
        	personObjectId = personId;
            rayLength = length;
        }

        public float addSingleResult(CollisionWorld.LocalRayResult rayResult, boolean normalInWorldSpace) {

            float dist = rayResult.hitFraction * rayLength;

            // check if we hit the person first
            if (!objectDetected && ((EntityInfo)rayResult.collisionObject.getUserPointer()).getObjectId() == personObjectId) {
                personDetected = true;
            }

            objectDetected = true;
            
            return rayResult.hitFraction;
        }
        
        public boolean isPersonDetected() {
            return personDetected;
        }
    }
}
