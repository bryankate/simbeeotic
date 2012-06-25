package harvard.robobees.simbeeotic.algorithms;


import harvard.robobees.simbeeotic.util.Gnuplotter2;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
//import Jama.*;

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
