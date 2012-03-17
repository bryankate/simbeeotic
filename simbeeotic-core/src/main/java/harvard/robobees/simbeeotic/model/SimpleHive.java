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


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.vecmath.Vector3f;
import java.awt.*;


/**
 * A hive implementation represented by a static box in the
 * world. Users can add sensors, a radio, and custom logic to
 * give it more interesting behavior.
 *
 * @author bkate
 */
public class SimpleHive extends GenericModel {

    // parameters
    private float size = 1.0f;  // m


    /** {@inheritDoc} */
    @Override
    protected RigidBody initializeBody(DiscreteDynamicsWorld world) {

        // establish the static hive geometry
        float halfSize = size / 2;

        CollisionShape colShape = new BoxShape(new Vector3f(halfSize, halfSize, halfSize));

        Transform startTransform = new Transform();
        startTransform.setIdentity();

        Vector3f start = getStartPosition();
        start.z += halfSize;

        startTransform.origin.set(start);

        getMotionRecorder().updateShape(getObjectId(), colShape);
        getMotionRecorder().updateMetadata(getObjectId(), new Color(166, 128, 100, 128), null, getName());

        int id = getObjectId();
        MotionState myMotionState = new RecordedMotionState(id, getMotionRecorder(), startTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0, myMotionState,
                                                                         colShape, new Vector3f(0, 0, 0));

        RigidBody body = new RigidBody(rbInfo);
        body.setUserPointer(new EntityInfo(id));

        world.addRigidBody(body, COLLISION_HIVE, COLLISION_NONE);

        return body;
    }


    /** {@inheritDoc} */
    @Override
    public void finish() {
    }


    public final float getSize() {
        return size;
    }


    @Override
    public String toString() {
        return "SimpleHive " + getModelId() + " " + getObjectId();
    }


    @Inject(optional = true)
    public final void setSize(@Named("size") final float size) {

        if (!isInitialized()) {
            this.size = size;
        }
    }
}
