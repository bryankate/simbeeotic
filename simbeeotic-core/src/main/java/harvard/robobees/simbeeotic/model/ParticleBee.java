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


import Jama.Matrix;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.algorithms.DefaultParticleFilter;
import harvard.robobees.simbeeotic.algorithms.ParticleFilter;
import harvard.robobees.simbeeotic.model.sensor.Compass;
import harvard.robobees.simbeeotic.model.sensor.LaserRangeSensor;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;

/**
 * A bee platform that implements localization with the help of a Particle filter and Laser Range Finder measurements.
 *
 * @author Mburkardt
 */

public class ParticleBee extends SimpleBee{

    private Compass compass;
    private LaserRangeSensor laserRangeSensor;
    public ParticleFilter particleFilter = new DefaultParticleFilter();

    private float maxVel = 1f; //set max velocity to 3 m/s, so that entire map can be mapped.
    private float[] range = new float[181];
    public float beeTheta;

    private static Logger logger = Logger.getLogger(OccupancyBee.class);
    public boolean move = true;
    public Matrix particles;
    public double[] w;
    public double[] z;
    double xSigma = .1;
    double ySigma = .1;
    double headingSigma = .1;
    int counter = 0;

    @Override
    public void initialize() {
        super.initialize();
        setHovering(true);  //make the bee hover at constant height
        //setUseRandomStart(true);

        compass = getSensor("compass", Compass.class); //compass to find heading
        laserRangeSensor = getSensor("range-sensor", LaserRangeSensor.class); //laser range finder for occupancy mapping
        //particleFilter = new DefaultParticleFilter();


        particleFilter.initialize();
//        turnToward(new Vector3f(100,100,1));
        turn((float)Math.PI/2);
        Vector3f pos = getTruthPosition();
        double xNoisy = pos.x + getRandom().nextGaussian()*xSigma;
        double yNoisy = pos.y + getRandom().nextGaussian()*ySigma;
        //logger.info("X for input: " + xNoisy + " Y for input: " + yNoisy);
        double headingNoisy = beeTheta + getRandom().nextGaussian()*headingSigma;

        //gives distance from the bee to the landmarks, with a little bit of noise.
        z = particleFilter.sense(xNoisy,yNoisy,headingNoisy);
        particles = particleFilter.generateParticles(1000);



    }


    @Override
    protected void updateKinematics(SimTime time) {
        Vector3f pos = getTruthPosition();
        Vector3f vel = getTruthLinearVelocity();




        beeTheta = compass.getHeading();

        logger.info("ID: " + getModelId() + "  " +
                "time: " + time.getImpreciseTime() + "  " +
                "pos: " + pos + "  " +
                "vel: " + vel + " " + "counter:" + counter);


        if (move == false){
            setDesiredLinearVelocity(new Vector3f(0,0,0));
            double xNoisy = pos.x + getRandom().nextGaussian()*xSigma;
            double yNoisy = pos.y + getRandom().nextGaussian()*ySigma;
            //logger.info("X for input: " + xNoisy + " Y for input: " + yNoisy);
            double headingNoisy = beeTheta + getRandom().nextGaussian()*headingSigma;
            z = particleFilter.sense(xNoisy,yNoisy,headingNoisy);
            w = particleFilter.measureProb(particles, z);
            particles = particleFilter.resample(particles,w, pos);
            counter++;
            move = true;

        }
        if (move == true){
            turn(0f);
            setDesiredLinearVelocity(new Vector3f(.5f,0,0));
            particles = particleFilter.moveParticles(particles,0,.05);
            move = false;
        }
        if (time.getImpreciseTime()%50 ==0){
            turn((float)Math.PI/4);
            particles = particleFilter.moveParticles(particles,Math.PI/4,0);
        }





    }


    @Override
    public void finish() {
    }


    @Inject(optional = true)
    public final void setMaxVelocity(@Named(value = "max-vel") final float vel) {
        this.maxVel = vel;
    }


}

