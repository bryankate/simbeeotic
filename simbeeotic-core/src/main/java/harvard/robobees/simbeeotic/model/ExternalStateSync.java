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


import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A class that acts as a go-between for an external source of
 * state information (e.g. motion tracking cameras) and
 * physics engine collision objects. The external source updates the
 * data and the collision object queries for the most recent state
 * periodically.
 *
 * @author bkate
 */
public class ExternalStateSync {

    private Map<Integer, CollisionObject> objects = new HashMap<Integer, CollisionObject>();
    private Map<Integer, Transform> states = new ConcurrentHashMap<Integer, Transform>();


    /**
     * Registers a collision object with the synchronizer. The object's state
     * is then updated by the external source.
     *
     * @param name The name of the object.
     * @param colObj The object to be synchronized.
     */
    public void registerObject(String name, CollisionObject colObj) {

        int id = name.hashCode();

        if (!objects.containsKey(id)) {
            objects.put(id, colObj);
        }
    }


    /**
     * Sets the state of an object using external data.
     *
     * @param name The ID of the object being updated.
     * @param position The new position of the object.
     * @param orientation The new orientation of the object.
     */
    public void setState(String name, Vector3f position, Quat4f orientation) {

        Transform trans = new Transform();

        trans.origin.set(position);
        trans.setRotation(orientation);
        
        states.put(name.hashCode(), trans);
    }


    /**
     * Updates the collision objects to reflect the latest external
     * kinematic state update.
     */
    public void updateStates() {

        for (int id : objects.keySet()) {

            Transform trans = states.get(id);

            if (trans != null) {
                objects.get(id).setWorldTransform(states.get(id));
            }
        }
    }
}
