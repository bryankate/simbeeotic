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


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;


/**
 * A class that implements some boilerplate functionality of radios
 * that operate in the 2.4 GHz range according to the IEEE 802.15.4
 * specification.
 *
 * @author bkate
 */
public abstract class ZigbeeRadio extends AbstractRadio {

    private Band band = new Band(2405, 5);  // channel 11

    private static Band operating = new Band(2442.5, 85);  // zigbee spectrum

    private static Logger logger = Logger.getLogger(RF230.class);


    /** {@inheritDoc} */
    @Override
    public final Band getOperatingBand() {
        return operating;
    }


    /**
     * Gets the current channel on which the radio is operating.
     *
     * @return The current channel.
     */
    public final Band getChannel() {
        return band;
    }


    /**
     * Sets the channel on which the radio is operating.
     *
     * @param channel The operating channel. It must be an integer in the range (11,26).
     */
    @Inject(optional = true)
    public final void setChannel(@Named(value = "channel") final int channel) {

        if ((channel < 11) || (channel > 26)) {

            logger.warn("Unrecognized channel, using default channel.");

            band = new Band(2405, 5);
        }

        band= new Band(2405 + (5 * (channel - 11)), 5);
    }

}
