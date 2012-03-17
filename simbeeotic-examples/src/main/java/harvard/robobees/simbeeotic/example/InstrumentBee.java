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
package harvard.robobees.simbeeotic.example;


import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.environment.PhysicalConstants;
import harvard.robobees.simbeeotic.model.EntityInfo;
import harvard.robobees.simbeeotic.model.RecordedMotionState;
import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.model.comms.Radio;
import harvard.robobees.simbeeotic.model.sensor.Accelerometer;
import harvard.robobees.simbeeotic.model.sensor.Compass;
import harvard.robobees.simbeeotic.model.sensor.ContactSensor;
import harvard.robobees.simbeeotic.model.sensor.Gyroscope;
import harvard.robobees.simbeeotic.model.sensor.RangeSensor;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.awt.Color;
import java.util.concurrent.TimeUnit;


/**
 * A bee that makes random adjustments to its movement at every time step
 * and takes a sensor reading periodically. It is used to define a typical
 * workload in instrumentation runs.
 *
 * @author bkate
 */
public class InstrumentBee extends SimpleBee {

    private Compass compass;
    private Radio radio;

    private float maxVelocity = 2.0f;                  // m/s
    private float velocitySigma = 0.2f;                // m/s
    private float headingSigma = (float)Math.PI / 16;  // rad
    private long sensorTimeout = 1000;                 // ms
    private long radioTimeout = 1000;                  // ms
    private boolean useRadio = false;

    private static Logger logger = Logger.getLogger(InstrumentBee.class);


    @Override
    public void initialize() {

        super.initialize();

        setHovering(true);

        compass = getSensor("compass", Compass.class);

        createTimer(new TimerCallback() {

            @Override
            public void fire(SimTime time) {
                
                compass.getHeading();
            }
        }, 0, TimeUnit.SECONDS, sensorTimeout, TimeUnit.MILLISECONDS);

        radio = getRadio();

        if (useRadio) {

            final byte[] packet = new byte[] {1, 2, 3, 4};

            createTimer(new TimerCallback() {

                @Override
                public void fire(SimTime time) {
                    radio.transmit(packet);
                }
            }, 0, TimeUnit.SECONDS, radioTimeout, TimeUnit.MILLISECONDS);
        }
    }


    @Override
    protected void updateKinematics(SimTime time) {

        // randomly vary the heading (rotation about the Z axis)
        turn((float)getRandom().nextGaussian() * headingSigma);

        // randomly vary the velocity in the X and Z directions
        Vector3f newVel = getDesiredLinearVelocity();

        newVel.add(new Vector3f((float)getRandom().nextGaussian() * velocitySigma,
                                0,
                                (float)getRandom().nextGaussian() * velocitySigma));

        // cap the velocity
        if (newVel.length() > maxVelocity) {

            newVel.normalize();
            newVel.scale(maxVelocity);
        }

        setDesiredLinearVelocity(newVel);

//        Vector3f pos = getTruthPosition();
//        Vector3f vel = getTruthLinearVelocity();
//
//        logger.info("ID: " + getModelId() + "  " +
//                    "time: " + time.getImpreciseTime() + "  " +
//                    "pos: " + pos + "  " +
//                    "vel: " + vel + " ");
    }


    @Override
    public void finish() {
    }


    @Inject(optional = true)
    public final void setMaxVelocity(@Named(value = "max-vel") final float vel) {
        this.maxVelocity = vel;
    }


    @Inject(optional = true)
    public final void setVelocitySigma(@Named(value = "vel-sigma") final float sigma) {
        this.velocitySigma = sigma;
    }


    @Inject(optional = true)
    public final void setHeadingSigma(@Named(value = "heading-sigma") final float sigma) {
        this.headingSigma = sigma;
    }


    @Inject(optional = true)
    public final void setSensorTimeout(@Named(value = "sensor-timeout") final long timeout) {
        this.sensorTimeout = timeout;
    }


    @Inject(optional = true)
    public final void setUserRadio(@Named(value = "use-radio") final boolean use) {
        this.useRadio = use;
    }


    @Inject(optional = true)
    public final void setRadioTimeout(@Named(value = "radio-timeout") final long timeout) {
        this.radioTimeout = timeout;
    }
}
