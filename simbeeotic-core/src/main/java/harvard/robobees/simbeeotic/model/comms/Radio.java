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


import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;


/**
 * An interface that describes a radio that is capable of sending and receiving data.
 *
 * @author bkate
 */
public interface Radio {

    /**
     * Transmits a message over the physical medium. The {@link PropagationModel}
     * in use will determine which radios, if any, receive the message.
     *
     * @param data The data to be transmitted.
     */
    public void transmit(byte[] data);


    /**
     * Transmits a message asynchronously over the physical medium. The {@link PropagationModel}
     * in use will determine which radios receive the message.
     *
     * @param data The data to be transmitted.
     *
     * @return True on success and false on failure. Failure occurs if the massage buffer
     *         in the radio is full.
     */
    public boolean transmitAsync(byte[] data);

    /**
     * Called by the {@link PropagationModel} when a transmission is received by this
     * radio.
     *
     * @param time The simulation time when the message was received.
     * @param data The data received.
     * @param rxPower The strength of the received signal (in dBm).
     * @param frequency The frequency of the received signal (in MHz).
     */
    public void receive(SimTime time, byte[] data, double rxPower, double frequency);


    /**
     * Gets the position of the radio.
     *
     * @return The position of the radio's antenna, in the world reference frame.
     */
    public Vector3f getPosition();


    /**
     * Gets the pointing vector of the antenna. This is a vector that points
     * along the major antenna axis.
     *
     * @return The pointing vector of the antenna, in the world reference frame.
     */
    public Vector3f getAntennaPointing();


    /**
     * Gets the vector that is normal to the antenna pointing vector. This
     * vector is essentially the {@code X axis} of the antenna frame translated
     * into the world frame.
     *
     * @return The antenna normal, in the world reference frame.
     */
    public Vector3f getAntennaNormal();


    /**
     * Gets the radiation pattern of the antenna attached to the radio.
     *
     * @return The antenna pattern in use.
     */
    public AntennaPattern getAntennaPattern();


    /**
     * Gets the RF band in which this radio operates. This method must return the full
     * range of possible frequencies, not just the channel in which it is currently
     * operating. The return from this method is expected to be static over time.
     * 
     * @return The full operating range of the radio.
     */
    public Band getOperatingBand();
}
