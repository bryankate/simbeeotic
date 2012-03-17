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


import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.AbstractModel;
import harvard.robobees.simbeeotic.model.Model;

import javax.vecmath.Vector3f;
import java.util.Random;


/**
 * A base class for sensors that holds basic position and pointing information
 * and provides convenience methods.
 *
 * @author bkate
 */
public abstract class AbstractSensor extends AbstractModel {

    private PhysicalEntity host;

    private Vector3f offset;
    private Vector3f pointing;


    /** {@inheritDoc} */
    public void finish() {
    }


    /**
     * Adds gaussian noise to a reading according to the given sigma value. This mechanism
     * is provided so that the values will be repeatable. This is possible because of the
     * properly seeded random number generator in this class.
     *
     * @param reading The reading to be noised.
     * @param sigma The variance of the Gaussian noise.
     *
     * @return The reading, with gaussian noise added.
     */
    protected final float addNoise(final float reading, final float sigma) {
        return reading + ((float)getRandom().nextGaussian() * sigma);
    }


    protected final PhysicalEntity getHost() {
        return host;
    }


    protected final Vector3f getOffset() {
        return offset;
    }


    protected final Vector3f getPointing() {
        return pointing;
    }


    /**
     * {@inheritDoc}
     *
     * This implementation ensures that the host model is a {@link PhysicalEntity}.
     */
    @Override
    public void setParentModel(Model parent) {

        super.setParentModel(parent);

        if (parent instanceof PhysicalEntity) {
            setHost((PhysicalEntity)parent);
        }
    }


    // this is only optional when wired up by the standard way (parent is a model that implements PhysicalEntity)
    @Inject(optional = true)
    public final void setHost(final PhysicalEntity host) {
        this.host = host;
    }


    @Inject
    public final void setOffset(@Named("offset") final Vector3f offset) {
        this.offset = offset;
    }


    @Inject
    public final void setPointing(@Named("pointing") final Vector3f pointing) {
        this.pointing = pointing;
    }
}
