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
package harvard.robobees.simbeeotic.model.comms;


import static harvard.robobees.simbeeotic.environment.PhysicalConstants.SPEED_OF_LIGHT;


/**
 * An RF propagation model that assumes line-of-sight and
 * uses the free space model for path loss:
 *
 * <ul>
 *   <li>http://en.wikipedia.org/wiki/Free-space_path_loss</li>
 *   <li>http://people.seas.harvard.edu/~jones/es151/prop_models/propagation.html#fsl</li>
 * </ul>
 *
 * @author bkate
 */
public class FreeSpacePropagationModel extends AbstractPropagationModel {

    private static final double MIN_DISTANCE = 0.01;


    /**
     * {@inheritDoc}
     *
     * Uses a free space model to calculate the path loss between the two radios.
     */
    @Override
    protected double calculatePathLoss(Radio tx, Radio rx, double txPower, Band band, double distance) {

        double lambda = SPEED_OF_LIGHT / (band.getCenterFrequency() * 10e6);  // m

        double rxPower = getReceivedPower(tx, rx, txPower);
        double loss;

        // degradation with free space model
        if (distance > MIN_DISTANCE) {
            loss = Math.pow(lambda / (4 * Math.PI * distance), 2);
        }
        else {

            // for very short distances, use simpler form
            loss = Math.pow(distance + 1, 2);
        }

        return rxPower + 10 * Math.log10(loss);
    }
}
