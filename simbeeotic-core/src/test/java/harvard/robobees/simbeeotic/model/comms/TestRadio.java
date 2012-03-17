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


import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.comms.AntennaPattern;
import harvard.robobees.simbeeotic.model.comms.PropagationModel;
import harvard.robobees.simbeeotic.model.comms.Band;
import harvard.robobees.simbeeotic.model.comms.Radio;
import harvard.robobees.simbeeotic.model.comms.IsotropicAntenna;

import javax.vecmath.Vector3f;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.LinkedList;


/**
 * @author bkate
 */
public class TestRadio implements Radio {

    private Vector3f position;
    private Vector3f pointing = new Vector3f(0, 0, 1);
    private Vector3f pointingNormal = new Vector3f(1, 0, 0);
    private AntennaPattern pattern = new IsotropicAntenna();
    private PropagationModel propModel;

    private List<Double> points = new LinkedList<Double>();

    private static Logger logger = Logger.getLogger(TestRadio.class);


    public TestRadio(Vector3f pos, PropagationModel prop) {

        position = pos;
        propModel = prop;
    }


    @Override
    public Vector3f getAntennaPointing() {
        return pointing;
    }


    @Override
    public Vector3f getAntennaNormal() {
        return pointingNormal;
    }


    @Override
    public Vector3f getPosition() {
        return position;
    }


    public void setPosition(Vector3f pos) {
        position = pos;
    }


    @Override
    public AntennaPattern getAntennaPattern() {
        return pattern;
    }


    @Override
    public void receive(SimTime time, byte[] data, double rxPower, double frequency) {

        points.add(rxPower);

        logger.debug("received message wih power: " + rxPower + " dBm ( " + MathUtil.dbmToMw(rxPower) + " mW )");
    }


    @Override
    public void transmit(byte[] data) {
        propModel.transmit(this, data, -25, new Band(2405, 5));
    }


    @Override
    public boolean transmitAsync(byte[] data) {

        transmit(data);
        return true;
    }


    @Override
    public Band getOperatingBand() {
        return new Band(2442.5, 85);
    }

    public List<Double> getReceivedData() {
        return points;
    }
}
