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
package harvard.robobees.simbeeotic.environment;


import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;
import java.util.Map;

import harvard.robobees.simbeeotic.util.BoundingSphere;


/**
 * A container class that holds information about an object in the physical world
 * (obstacles, flowers, etc).
 *
 * @author bkate
 */
public class WorldObject {

    private int objectId;
    private Type type;
    private RigidBody body;
    private Map<String, Object> meta;

    /**
     * Enumerates the types of physical objects that populate the world.
     */
    public static enum Type {

        TERRAIN,
        OBSTACLE,
        STRUCTURE,
        FLOWER,
        PERSON
    }


    public WorldObject(int objectId, Type type, RigidBody body, Map<String, Object> meta) {

        this.objectId = objectId;
        this.type = type;
        this.body = body;
        this.meta = meta;
    }


    /**
     * Gets the identifier of this physical object.
     *
     * @return The ID.
     */
    public int getObjectId() {
        return objectId;
    }


    /**
     * Gets the type of physical object.
     *
     * @return The enumerated type.
     */
    public Type getType() {
        return type;
    }


    /**
     * Gets the truth position of the object.
     *
     * @return The position of the object (in the world frame).
     */
    public Vector3f getTruthPosition() {
        return new Vector3f(body.getMotionState().getWorldTransform(new Transform()).origin);
    }


    /**
     * Gets the truth orientation of the object.
     *
     * @return The object's orientation (in the world frame).
     */
    public final Quat4f getTruthOrientation() {
        return body.getOrientation(new Quat4f());
    }


    /**
     * Gets the parameters of a sphere that encompasses the object.
     *
     * @return The bounding sphere for the object.
     */
    public BoundingSphere getTruthBoundingSphere() {

        Vector3f center = new Vector3f();
        float[] radPtr = new float[1];

        body.getCollisionShape().getBoundingSphere(center, radPtr);

        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(getTruthOrientation());

        trans.transform(center);

        center.add(getTruthPosition());

        return new BoundingSphere(center, radPtr[0]);
    }


    /**
     * Gets the metadata associated with this object.
     *
     * @return The object's metadata.
     */
    public Map<String, Object> getMetadata() {
        return meta;
    }
}
