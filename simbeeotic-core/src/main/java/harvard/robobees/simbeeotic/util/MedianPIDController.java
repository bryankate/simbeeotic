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


import org.apache.log4j.Logger;
//import sun.security.pkcs11.wrapper.CK_SSL3_MASTER_KEY_DERIVE_PARAMS;

import java.util.concurrent.TimeUnit;

/**
 * A PID control loop with a median filter on D.
 *
 * @author kar
 */
public class MedianPIDController extends PIDController {

    final int MFWIDTH = 3;
    protected double[] median;
    int mptr;
    double alpha, neg, pos;
    double mediand;
    double ferr, lferr;

    private static Logger logger = Logger.getLogger(MedianPIDController.class);

    public MedianPIDController(double set, double p, double i, double d, double a, double na, double pa) {
        super(set, p, i, d);
        alpha = a;
        neg = na;
        pos = pa;
        median = new double[MFWIDTH];
        for( int ii = 0; ii < MFWIDTH; ii++ )
            median[ii] = 0;
        mptr=0;
        mediand = 0.0;
        ferr = 0.0;
        lferr = 0.0;
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

        double error = setPoint - currValue;

        if (lastTime == 0) {

            lastTime = currTime;
            lastError = error;

            lferr = error;
            ferr = error;

            for( int ii = 0; ii < MFWIDTH; ii++ )
                median[ii] = 0;

//            return null;
        }
        else
            ferr = alpha*error + (1.0 - alpha)*ferr;

        double dt = (double)(currTime - lastTime) / TimeUnit.SECONDS.toNanos(1);

        double fderiv = 0;
        if (dt > 0) {
            fderiv = (ferr - lferr) / dt;
        }

        double mval = 0.0;
        median[mptr] = fderiv;
        mptr = (mptr + 1)%MFWIDTH;

        if((median[0] > median[1]) && (median[0] > median[2])) {
            if(median[1] > median[2])
                mval = median[1];
            else
                mval = median[2];
        } else if((median[1] > median[0]) && (median[1] > median[2])) {
            if(median[0] > median[2])
                mval = median[0];
            else
                mval = median[2];
        } else if((median[2] > median[1]) && (median[2] > median[0])) {
            if(median[1] > median[0])
                mval = median[1];
            else
                mval = median[0];
        }

//        mediand += alpha * (mval - mediand);
        mediand = mval;

        logger.debug("realtime: " + System.currentTimeMillis() + " dt: " + dt + " deriv: " + fderiv);
        if(mediand * error > 0.0)
            integral += error * dt;
        lastTime = currTime;
        lastError = error;
        lferr = ferr;

        double ret = (pGain * error) + (iGain * integral) + (dGain * mediand);

        return ret < 0 ? neg*ret : pos*ret;
    }

    public double getDErr() {
        return mediand;
    }

    public double getIErr() {
        return integral;
    }

    public double getSetPoint() {
        return setPoint;
    }

    public void setIErr(double ierr) {
        integral = ierr;
    }
}
