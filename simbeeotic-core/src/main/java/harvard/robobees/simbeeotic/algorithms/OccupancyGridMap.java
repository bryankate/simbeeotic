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

package harvard.robobees.simbeeotic.algorithms;


import harvard.robobees.simbeeotic.util.Gnuplotter2;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
//import Jama.*;


/**
 * @author Mburkardt
 */

public class OccupancyGridMap {
    /*
    map m
    poses x
    measurements m
    */

    private static Logger logger = Logger.getLogger(OccupancyGridMap.class);

    float mGrid[][] = new float[100][100];
    float maxSensorRange = 15; //this is the value i found in DefaultLaserRangeSensor
    Gnuplotter2 polar = new Gnuplotter2(true);

    //todo: observe


    //todo: given a pose, figure out what cells are occupado, increment count of each cell in
    //todo: preception field of sensor reading. If occupado, increment grid[][], otherwise decrement it.
    //if count[]][]==0, we don't know anything about it yet.
    //if count>0, grid>0, probably occupado
    //if count>0, grid<0, probably empty

    public void initialize(){
        polar.setProperty("term", "x11");
        polar.unsetProperty("key");
        polar.setProperty("title", "'Polar representation of what the bee sees. +x direction is direction of travel.'");
        polar.setPlotParams("with points");
        //polar.setPlotParams("Xrange", "[-15,15]");



    }

    public void polarMap(float range[], float beeTheta){

        //Vector3f beePose = new Vector3f(beePosition.x, beePosition.y, beeTheta); //x,y,theta
        polar.clearData();

        for (int h=0; h<181; h++){
            float heading = beeTheta + 90 - h;  //gives angle from true north for each point in the beam
            double angle= Math.PI*heading/180.0;
            if (range[h] == Float.POSITIVE_INFINITY){
                range[h] = maxSensorRange;
            }
            double xPoint = range[h]* Math.cos(angle);
            double yPoint = range[h]* Math.sin(angle);
            String lineData = (xPoint+ " " + yPoint + "\n ");
            logger.info("x=" + xPoint + " y="+ yPoint + " heading=" + heading + " h=" +h);
            polar.addDataPoint(lineData);



        }



        polar.plot();
    }

    /*
    for x-maxSensorRange < x < x+maxSensorRange {
        for y-maxSensorRange < y < y+maxSensorRange {
            (r, theta) = Math.atan2(y,x);
            thetaRound = round theta;
            //transformTheta = theta in bee's coords, so that it matches the indecies of h.
            if (r<range[h]){
                show that this is free, maybe by giving value of .2?
            }
        }
    }

     */
    /*for (int x=-50; x<51; x++){
                for (int y=-50; y<51; y++){
                    double r;
                    double theta;
                    r = Math.sqrt((x-xPoint)*(x-xPoint)+(y-yPoint)*(y-yPoint));
                    theta = Math.atan2(y,x)*180/Math.PI;
                    int phi = (int)Math.round(theta+beeTheta) +180;
                    if (phi>181 && phi<-1){
                       if (range[phi] == Float.POSITIVE_INFINITY){
                            range[phi] = maxSensorRange;
                        }
                        if (r<range[phi]){
                            int xEmpty = (int) Math.round(range[phi]*Math.cos(theta*Math.PI/180));
                            int yEmpty = (int) Math.round(range[phi]*-Math.sin(theta*Math.PI/180));
                            String emptyData = (xEmpty + " " + yEmpty + " " + 1 + "\n ");
                            polar.addDataPoint(emptyData);
                        }


                    }
                }

            }*/


    public void occupancyGrid(){

    }


}
