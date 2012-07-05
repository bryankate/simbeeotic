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
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.algorithms.EkfSlam;
import harvard.robobees.simbeeotic.algorithms.StaticOccupancyMap;
import harvard.robobees.simbeeotic.model.sensor.*;
import harvard.robobees.simbeeotic.util.HeatMap;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.security.PrivateKey;

/**
 * A bee that performs occupancy grid mapping with the use of a laser range finder.
 *
 * @author Mburkardt
 */

public class OccupancyBee extends SimpleBee{

    private Compass compass;
    private LaserRangeSensor laserRangeSensor;
    private StaticOccupancyMap occupancyMap = new StaticOccupancyMap();


    private EkfSlam kalmantester = new EkfSlam();


    private float maxVel = 1f; //set max velocity to 3 m/s, so that entire map can be mapped.
    private float[] range = new float[181];
    public float beeTheta;
    public int counter = 0;

    private static Logger logger = Logger.getLogger(OccupancyBee.class);



    @Override
    public void initialize() {
        super.initialize();
        setHovering(true);  //make the bee hover at constant height

        compass = getSensor("compass", Compass.class); //compass to find heading
        laserRangeSensor = getSensor("range-sensor", LaserRangeSensor.class); //laser range finder for occupancy mapping

        occupancyMap.initialize();

        //kalmantester.xythetaKalman();


    }


    @Override
    protected void updateKinematics(SimTime time) {
        Vector3f pos = getTruthPosition();
        Vector3f vel = getTruthLinearVelocity();

        beeTheta = compass.getHeading();

        logger.info("ID: " + getModelId() + "  " +
                "time: " + time.getImpreciseTime() + "  " +
                "pos: " + pos + "  " +
                "vel: " + vel + " ");


        range = laserRangeSensor.getRange();  //get range from the laser-range-finder.



        //occupancyMap.drawRange(range);
        //occupancyMap.polarMap(range, beeTheta);
        //occupancyMap.occupancy(range, pos, beeTheta);
        occupancyMap.bayesOccupancy(range, pos, beeTheta);
        //occupancyMap.scaledLaserHeading(range, beeTheta);


        //occupancyMap.slamMap(range, pos, beeTheta);


        Vector3f difference = new Vector3f();
        Vector3f zeroZeroZero = new Vector3f(0,0,0);

	//move the bee in C-shape, mapping the entire hallway.



       /* if (counter==0){
            Vector3f target = new Vector3f(0,-40,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
                return;
            }
        }


        else if (counter==1){
            Vector3f target = new Vector3f(-25,-40,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
            }
        }
        else if (counter==2){
            Vector3f target = new Vector3f(0,-40,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
            }
        }
        else if (counter==3){
            Vector3f target = new Vector3f(0,40,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
            }
        }
        else if (counter==4){
            Vector3f target = new Vector3f(-25,40,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
            }
        }

        else if (counter==5){
            Vector3f target = new Vector3f(0, 40, 1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                finish();
            }

        }

        */

        //turn((float).1);


        //make bee move along Dworkin Hallways
        /*if (counter==0){
            Vector3f target = new Vector3f(35,-1,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
                return;
            }
        }


        else if (counter==1){
            Vector3f target = new Vector3f(-16,-1,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
            }
        }
        else if (counter==2){
            Vector3f target = new Vector3f(-17,12,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
            }
        }
        else if (counter==3){
            Vector3f target = new Vector3f(-26,12,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
            }
        }
        else if (counter==4){
            Vector3f target = new Vector3f(5,12,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                finish();
            }
        } */


        if (counter==0){
            Vector3f target = new Vector3f(-1,12,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
                return;
            }
        }
        else if (counter==1){
            Vector3f target = new Vector3f(-25,12,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
                return;
            }
        }
        else if (counter==2){
            Vector3f target = new Vector3f(-16.3f,12,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
                return;
            }
        }
        else if (counter==3){
            Vector3f target = new Vector3f(-16.3f,-1,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                counter++;
                return;
            }
        }
        else if (counter==4){
            Vector3f target = new Vector3f(35,-1,1);
            turnToward(target);
            Vector3f velDesire = new Vector3f(maxVel, 0, 0);
            setDesiredLinearVelocity(velDesire);

            difference.sub(target, pos);
            double distToGo = difference.length();
            if (distToGo < 1){
                finish();
            }
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
