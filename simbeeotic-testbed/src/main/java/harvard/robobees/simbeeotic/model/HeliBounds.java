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


import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * A model that holds the physical bounds of the helicopters in the simulation. Each
 * helicopter can lookup this model to get the bounds.
 *
 * @author bkate
 */
public class HeliBounds extends AbstractModel {

    // state
    private Boundary bounds;

    // params
    private float xMin = Boundary.DEFAULT_X_MIN;
    private float xMax = Boundary.DEFAULT_X_MAX;
    private float yMin = Boundary.DEFAULT_Y_MIN;
    private float yMax = Boundary.DEFAULT_Y_MAX;
    private float zMax = Boundary.DEFAULT_Z_MAX;


    @Override
    public void finish() {
    }


    public Boundary getBounds() {

        if (bounds == null) {
            bounds = new Boundary(xMin, xMax, yMin, yMax, zMax);
        }

        return bounds;
    }


    @Inject(optional = true)
    public final void setXMin(@Named("min-x-bound") final float bound) {
        this.xMin = bound;
    }


    @Inject(optional = true)
    public final void setXMax(@Named("max-x-bound") final float bound) {
        this.xMax = bound;
    }


    @Inject(optional = true)
    public final void setYMin(@Named("min-y-bound") final float bound) {
        this.yMin = bound;
    }


    @Inject(optional = true)
    public final void setYMax(@Named("max-y-bound") final float bound) {
        this.yMax = bound;
    }


    @Inject(optional = true)
    public final void setZMax(@Named("max-z-bound") final float bound) {
        this.zMax = bound;
    }
}
