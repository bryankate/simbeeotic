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
package harvard.robobees.simbeeotic.example;


import org.apache.log4j.Logger;
import harvard.robobees.simbeeotic.model.SimpleBee;
import harvard.robobees.simbeeotic.model.Timer;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.model.comms.MessageListener;
import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;
import java.util.concurrent.TimeUnit;


/**
 * A bee that broadcasts a message at every timestep and records the messages
 * that it receives.
 * 
 * @author bkate
 */
public class BroadcastBee extends SimpleBee implements MessageListener {

    private static Logger logger = Logger.getLogger(BroadcastBee.class);


    @Override
    public void initialize() {

        super.initialize();

        // set some initial direction
        setHovering(true);
        setDesiredLinearVelocity(new Vector3f((float)getRandom().nextGaussian() * 0.1f,
                                              (float)getRandom().nextGaussian() * 0.1f,
                                              (float)getRandom().nextGaussian() * 0.1f));

        getRadio().addMessageListener(this);

        // send a message every second
        Timer msgTimer = createTimer(new TimerCallback() {

            public void fire(SimTime time) {
                getRadio().transmit(("" + getModelId()).getBytes());
            }
        }, 0, TimeUnit.MILLISECONDS, 1, TimeUnit.SECONDS);
    }


    @Override
    protected void updateKinematics(SimTime time) {

        Vector3f pos = getTruthPosition();

        logger.info("ID: " + getModelId() + "  " +
                    "time: " + time.getImpreciseTime() + "  " +
                    "pos: " + pos);
    }


    public void messageReceived(SimTime time, byte[] data, double rxPower) {

        logger.info("ID: " + getModelId() + "  " +
                    "time: " + time.getImpreciseTime() + "  " +
                    "power: " + rxPower + "  (dBm) " +
                    "recv from: " + new String(data));
    }


    @Override
    public void finish() {
    }
}
