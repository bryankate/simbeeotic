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


import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public interface Accelerometer {

    /**
     * <p>
     * Gets the linear acceleration of the body, as measured by
     * a 3-axis accelerometer. The readings are relative to the body frame,
     * meaning that the reading in the x direction is aligned down the
     * x axis of the body, regardless of the body's orientation in the
     * physical world.
     * </p>
     * <p>
     * The accelerometer is "calibrated" to return the
     * acceleration of the object relative to the global inertial frame
     * (the Earth). This means that an object at rest will record an
     * acceleration with magnitude 0g, whereas an object in freefall
     * will experience an acceleration of -1g. Note that this is
     * different from the raw readings recorded by many real world
     * accelerometers (1g at rest, 0g in freefall) that are not
     * calibrated for use in an intertial navigation scenario.
     * </p>
     *
     * @return The acceleration along each body axis, (x, y, z), in m/s^2.
     */
    public Vector3f getLinearAcceleration();
}
