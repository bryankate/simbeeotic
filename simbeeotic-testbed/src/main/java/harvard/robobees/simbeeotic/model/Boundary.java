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


/**
 * A class that represents the physical bounds in which the helicopters can fly.
 *
 * @author bkate
 */
public class Boundary {

    private float xMin = DEFAULT_X_MIN;
    private float xMax = DEFAULT_X_MAX;
    private float yMin = DEFAULT_Y_MIN;
    private float yMax = DEFAULT_Y_MAX;
    private float zMax = DEFAULT_Z_MAX;

    public static final float DEFAULT_X_MIN = -2.3f;
    public static final float DEFAULT_X_MAX = 2.3f;
    public static final float DEFAULT_Y_MIN = -3.7f;
    public static final float DEFAULT_Y_MAX = 3.7f;
    public static final float DEFAULT_Z_MAX = 2f;


    public Boundary() {
        // use default settings
    }


    public Boundary(float xMin, float xMax, float yMin, float yMax, float zMax) {

        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.zMax = zMax;
    }


    public float getXMax() {
        return xMax;
    }


    public float getXMin() {
        return xMin;
    }


    public float getYMax() {
        return yMax;
    }


    public float getYMin() {
        return yMin;
    }


    public float getZMax() {
        return zMax;
    }


    public float getRadius() {
        return Math.max(xMax, yMax);
    }
}
