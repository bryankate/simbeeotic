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
package harvard.robobees.simbeeotic.component;


import harvard.robobees.simbeeotic.model.MotionRecorder;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.configuration.Variation;
import harvard.robobees.simbeeotic.ClockControl;
import harvard.robobees.simbeeotic.SimEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * The context in which {@link VariationComponent}s are executed.
 *
 * @author bkate
 */
public class VariationContext {

    @Inject
    @Named("variation-number")
    private int variationNum;

    @Inject
    private Variation variation;

    @Inject
    @GlobalScope
    private MotionRecorder recorder;

    @Inject(optional = true)
    @GlobalScope
    private ClockControl clockControl;

    @Inject(optional = true)
    @GlobalScope
    private SimEngine simEngine;


    /**
     * Gets the scenario variation number.
     *
     * @return The number of the variation of this scenario.
     */
    public final int getVariationNum() {
        return variationNum;
    }


    /**
     * Gets the scenario variation details.
     *
     * @return The details of this scenario variation.
     */
    public final Variation getVariation() {
        return variation;
    }


    /**
     * Gets the motion recorder, which can be used to obtain motion updates for objects in the simulation.
     *
     * @return The motion recorder.
     */
    public final MotionRecorder getRecorder() {
        return recorder;
    }


    /**
     * Gets the class that controls the state of the simulation clock.
     *
     * @return The clock control class for this variation.
     */
    public final ClockControl getClockControl() {
        return clockControl;
    }


    /**
     * Gets the simulation engine, which can be used to find models.
     *
     * @return The simulation engine in use for this variation.
     */
    public SimEngine getSimEngine() {
        return simEngine;
    }
}
