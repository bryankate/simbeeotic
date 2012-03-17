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


import harvard.robobees.simbeeotic.model.sensor.Accelerometer;
import harvard.robobees.simbeeotic.model.sensor.Compass;
import harvard.robobees.simbeeotic.model.sensor.ContactSensor;
import harvard.robobees.simbeeotic.model.sensor.Gyroscope;
import harvard.robobees.simbeeotic.model.sensor.RangeSensor;
import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class TestingBee extends SimpleBee {

    private Accelerometer accelerometer;
    private Gyroscope gyro;
    private Compass compass;
    private RangeSensor rangeBottom;
    private ContactSensor contactBottom;


    @Override
    public void initialize() {

        super.initialize();

        accelerometer = getSensor("accelerometer", Accelerometer.class);
        gyro = getSensor("gyro", Gyroscope.class);
        compass = getSensor("compass", Compass.class);
        rangeBottom = getSensor("rangeBottom", RangeSensor.class);
        contactBottom = getSensor("contactBottom", ContactSensor.class);
    }


    @Override
    protected void updateKinematics(SimTime time) {

        Vector3f pos = getTruthPosition();
        Vector3f linVel = getTruthLinearVelocity();
        Vector3f angVel = getTruthAngularVelocity();
        Vector3f linAccel = getTruthLinearAcceleration();
        Vector3f angAccel = getTruthAngularAcceleration();
        Vector3f orient = MathUtil.quaternionToEulerZYX(getTruthOrientation());

        float heading = compass.getHeading();
        Vector3f accelSens = accelerometer.getLinearAcceleration();
        Vector3f gyroSens = gyro.getAngularVelocity();
        float rangeSens = rangeBottom.getRange();
        boolean contactSens = contactBottom.isTripped();

        System.out.println("ID: " + getModelId() + "  " +
                           "time: " + time + "  " +
                           "pos: " + pos.x + " " + pos.y + " " + pos.z + "  " +
                           "linVel: " + linVel.x + " " + linVel.y + " " + linVel.z + "  " +
                           "angVel: " + angVel.x + " " + angVel.y + " " + angVel.z + "  " +
                           "linAccel: " + linAccel.x + " " + linAccel.y + " " + linAccel.z + "  " +
                           "angAccel: " + angAccel.x + " " + angAccel.y + " " + angAccel.z + "  " +
                           "orient: " + orient.x + " " + orient.y + " " + orient.z + "  " +
                           "accelerometer: " + accelSens.x + " " + accelSens.y + " " + accelSens.z + "  " +
                           "gyro: " + gyroSens.x + " " + gyroSens.y + " " + gyroSens.z + "  " +
                           "heading: " + heading + "  " +
                           "range: " + rangeSens + "  " +
                           "contact: " + contactSens + "  " +
                           "active: " + isActive());
    }


    @Override
    public void finish() {
    }
}
