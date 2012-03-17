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


import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.model.sensor.Accelerometer;
import harvard.robobees.simbeeotic.model.sensor.Gyroscope;
import harvard.robobees.simbeeotic.model.sensor.Compass;
import harvard.robobees.simbeeotic.model.sensor.RangeSensor;
import harvard.robobees.simbeeotic.model.sensor.ContactSensor;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.util.TracePlotter2D;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;


/**
 * A bee that makes random adjustments to its movement at every time step.
 *
 * @author bkate
 */
public class RandomWalkBee extends SimpleBee {

    private Accelerometer accelerometer;
    private Gyroscope gyro;
    private Compass compass;
    private RangeSensor rangeBottom;
    private ContactSensor contactBottom;

    private float maxVelocity = 2.0f;                  // m/s
    private float velocitySigma = 0.2f;                // m/s
    private float headingSigma = (float)Math.PI / 16;  // rad

    private static Logger logger = Logger.getLogger(RandomWalkBee.class);


    @Override
    public void initialize() {

        super.initialize();

        setHovering(true);

        accelerometer = getSensor("accelerometer", Accelerometer.class);
        gyro = getSensor("gyro", Gyroscope.class);
        compass = getSensor("compass", Compass.class);
        rangeBottom = getSensor("rangeBottom", RangeSensor.class);
        contactBottom = getSensor("contactBottom", ContactSensor.class);
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

        Vector3f pos = getTruthPosition();
        Vector3f vel = getTruthLinearVelocity();

        logger.info("ID: " + getModelId() + "  " +
                    "time: " + time.getImpreciseTime() + "  " +
                    "pos: " + pos + "  " +
                    "vel: " + vel + " ");
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
}
