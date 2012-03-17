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
package com.bulletphysics.dynamics;


import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.TransformUtil;

import javax.vecmath.Vector3f;
import java.util.concurrent.TimeUnit;


/**
 * A rigid body that does not participate in the state integration
 * performed by the dynamics world. The position and orientation is
 * derived from an external source. As such, the ability to apply forces directly
 * to the body has been deactivated.
 * <p/>
 * Because this is meant to be used with an external source, it is implicitly
 * coupled to real-time. Usage in a non-real-time setting is undefined for
 * some operations (e.g. calculating velocities from position).
 *
 * @author BNK
 * @author bkate
 */
public class ExternalRigidBody extends RigidBody {

    private long lastTime = Long.MIN_VALUE;
    private Vector3f linVel = new Vector3f();
    private Vector3f angVel = new Vector3f();
    private Vector3f linAccel = new Vector3f();
    private Vector3f angAccel = new Vector3f();

    private static final float SECONDS_TO_NANOS = TimeUnit.SECONDS.toNanos(1);


    public ExternalRigidBody(RigidBodyConstructionInfo constructionInfo) {
        super(constructionInfo);
    }


    public ExternalRigidBody(float mass, MotionState motionState, CollisionShape collisionShape) {
        this(mass, motionState, collisionShape, new Vector3f(0f, 0f, 0f));
    }


    public ExternalRigidBody(float mass, MotionState motionState, CollisionShape collisionShape, Vector3f localInertia) {
        this(new RigidBodyConstructionInfo(mass, motionState, collisionShape, localInertia));
    }


    public Vector3f getAngularAcceleration(Vector3f out) {

        out.set(angAccel);

        return out;
    }


    public Vector3f getLinearAcceleration(Vector3f out) {

        out.set(linAccel);

        return out;
    }


    @Override
    public void applyCentralForce(Vector3f force) {
    }


    @Override
    public void applyCentralImpulse(Vector3f impulse) {
    }


    @Override
    public void applyDamping(float timeStep) {
    }


    @Override
    public void applyForce(Vector3f force, Vector3f rel_pos) {
    }


    @Override
    public void applyGravity() {
    }


    @Override
    public void applyImpulse(Vector3f impulse, Vector3f rel_pos) {
    }


    @Override
    public void applyTorque(Vector3f torque) {
    }


    @Override
    public void applyTorqueImpulse(Vector3f torque) {
    }


    @Override
    public void setWorldTransform(Transform worldTransform) {

        long time = System.nanoTime();

        if (lastTime < 0) {
            lastTime = time;
        }

        float diff = (time - lastTime) / SECONDS_TO_NANOS;

        // calculate the velocities and accelerations
        if (diff > 0) {

            Transform old = new Transform(this.worldTransform);
            Vector3f oldLinVel = new Vector3f(linVel);
            Vector3f oldAngVel = new Vector3f(angVel);

            TransformUtil.calculateVelocity(old, worldTransform, diff, linVel, angVel);

            linAccel.sub(linVel, oldLinVel);
            linAccel.scale(1 / diff);

            angAccel.sub(angVel, oldAngVel);
            angAccel.scale(1 / diff);

            setLinearVelocity(linVel);
            setAngularVelocity(angVel);
        }
        else {

            linAccel = new Vector3f();
            angAccel = new Vector3f();
        }

//        lastTime = time;

        // set the transform
        super.setWorldTransform(worldTransform);

        // activate the body so that MotionStates are updated
        activate();
    }
}
