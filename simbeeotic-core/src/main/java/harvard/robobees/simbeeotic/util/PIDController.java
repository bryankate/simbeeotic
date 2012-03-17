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
package harvard.robobees.simbeeotic.util;


import java.util.concurrent.TimeUnit;


/**
 * A simple PID control loop.
 *
 * @author bkate
 */
public class PIDController {

    private double setPoint;
    private double pGain;
    private double iGain;
    private double dGain;

    private long lastTime;
    private double lastError = 0;
    private double integral = 0;


    /**
     * Establishes a new controller with a given setpoint and gains.
     *
     * @param set The target point.
     * @param p The proportional gain.
     * @param i The integral gain.
     * @param d The derivative gain.
     */
    public PIDController(double set, double p, double i, double d) {

        reset(set);

        pGain = p;
        iGain = i;
        dGain = d;
    }


    /**
     * Updates the controller with the current time and value.
     *
     * @param currTime The current time (in nanoseconds).
     * @param currValue The current value.
     *
     * @return The PID output.
     */
    public Double update(long currTime, double currValue) {

        if (lastTime == 0) {

            lastTime = currTime;
            lastError = setPoint - currValue;

            return null;
        }

        double dt = (double)(currTime - lastTime) / TimeUnit.SECONDS.toNanos(1);

        if (dt == 0) {
            return null;
        }

        double error = setPoint - currValue;
        double deriv = (error - lastError) / dt;

        integral += error * dt;
        lastTime = currTime;
        lastError = error;

        return (pGain * error) + (iGain * integral) + (dGain * deriv);
    }


    /**
     * Establishes a new setpoint for the PID controller to achieve.
     *
     * @param set The new target point.
     */
    public void setSetpoint(double set) {
        reset(set);
    }


    /**
     * Resets the PID controller to target a new setpoint. This includes
     * zeroing the accumulated integral and historical error value.
     *
     * @param set The new setpoint.
     */
    private void reset(double set) {

        setPoint = set;

        lastTime = 0;
        lastError = 0;
        integral = 0;
    }
}
