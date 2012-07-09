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
import harvard.robobees.simbeeotic.algorithms.*;
import harvard.robobees.simbeeotic.model.sensor.*;
import harvard.robobees.simbeeotic.util.HeatMap;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * A bee that performs occupancy grid mapping with the use of a laser range finder.
 *
 * @author Mburkardt
 */

public class SlamBee extends SimpleBee{

    private Compass compass;
    private LaserRangeSensor laserRangeSensor;



    public FastSlamInterface slam = new FastSlam();


    private float maxVel = 1f; //set max velocity to 3 m/s, so that entire map can be mapped.
    private float[] range = new float[181];
    public float beeTheta;
    public int counter = 0;
    private static Logger logger = Logger.getLogger(SlamBee.class);



    public Vector3f startPos;
    public double startTheta;
    //public Matrix measurements = new Matrix(new double[][] {{5,10}});  //landmarkCoords
    public Matrix measurements = new Matrix(new double[][] {{5,Math.PI/2,0}});
    public Matrix covariance;
    public Matrix stateVector;
    public Matrix controls;



    @Override
    public void initialize() {
        super.initialize();
        setHovering(true);  //make the bee hover at constant height
        turnToward(new Vector3f(1,0,1));
        compass = getSensor("compass", Compass.class); //compass to find heading
        laserRangeSensor = getSensor("range-sensor", LaserRangeSensor.class); //laser range finder for occupancy mapping

        startPos = getTruthPosition();
        startTheta = compass.getHeading();

        controls = new Matrix(new double[][] {{0},{0},{1}});

        //currently, measurements consists of x,y coords of the landmarks
        //slam.initialize();
        //slam.ekfPredict(controls);
        //stateVector = slam.getStateVector();
        //covariance = slam.getCovariance();
        Matrix newLandmark = new Matrix(new double[][] {{10,0}});

        slam.initializeEKF();
        slam.addNewLandmark(newLandmark);
        stateVector = slam.getStateVector();
        covariance = slam.getCovariance();
        slam.predict(controls);

        stateVector = slam.getStateVector();
        logger.info("StateVector: " + stateVector.get(0,0) + " " + stateVector.get(1,0) + " "
                + stateVector.get(2,0) + " " + stateVector.get(3,0) + " " + stateVector.get(4,0) + " " + stateVector.get(5,0));


        measurements = measurements.transpose();
        slam.updateOldLandmark(measurements);
        stateVector = slam.getStateVector();
        covariance = slam.getCovariance();
        logger.info("StateVector: " + stateVector.get(0,0) + " " + stateVector.get(1,0) + " "
            + stateVector.get(2,0) + " " + stateVector.get(3,0) + " " + stateVector.get(4,0) + " " + stateVector.get(5,0));



    }


    @Override
    protected void updateKinematics(SimTime time) {
        setDesiredLinearVelocity(new Vector3f(maxVel,0,0));
        Vector3f pos = getTruthPosition();
        Vector3f vel = getTruthLinearVelocity();
        turn(.1f);
        //Vector3f angVel = getTruthAngularVelocity();
        double  angVel = 1;

        //controls = new Matrix(new double[][] {{vel.x},{vel.y},{angVel.z}});
        controls = new Matrix(new double[][] {{vel.x},{vel.y},{angVel}});

        beeTheta = compass.getHeading();

        slam.predict(controls);
        slam.updateOldLandmark(measurements);
        stateVector = slam.getStateVector();
        covariance = slam.getCovariance();
        logger.info("StateVector: " + stateVector.get(0,0) + " " + stateVector.get(1,0) + " "
                + stateVector.get(2,0) + " " + stateVector.get(3,0) + " " + stateVector.get(4,0) + " " + stateVector.get(5,0));



        logger.info("ID: " + getModelId() + "  " +
                "time: " + time.getImpreciseTime() + "  " +
                "pos: " + pos + "  " +
                "vel: " + vel + " " + "angVel: " + angVel);


        //double x = pos.x - startPos.x;
        //double y = pos.y - startPos.y;
        //double theta = beeTheta - startTheta;
        //stateVector.set(0,0,x);
        //stateVector.set(1,0,y);
        //stateVector.set(2,0,theta);

        //slam.ekfPredict(controls);
        //if (counter == 0){
        //    slam.addLandmarks(measurements, controls);
        //    counter++;
        //}
        //slam.ekfUpdate(controls, measurements, 0);

    }


    @Override
    public void finish() {
    }


    @Inject(optional = true)
    public final void setMaxVelocity(@Named(value = "max-vel") final float vel) {
        this.maxVel = vel;
    }


}
