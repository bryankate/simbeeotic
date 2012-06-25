/**
 * @author Mburkardt
 */


package harvard.robobees.simbeeotic.algorithms;


import com.bulletphysics.linearmath.Transform;
import harvard.robobees.simbeeotic.algorithms.util.KalmanXYThetaEstimate;
import harvard.robobees.simbeeotic.model.sensor.DefaultLaserRangeSensor;
import harvard.robobees.simbeeotic.util.Gnuplotter2;
import harvard.robobees.simbeeotic.util.HeatMap;
import org.apache.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import Jama.Matrix;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.VectorRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;

import org.omg.CORBA.PUBLIC_MEMBER;
import harvard.robobees.simbeeotic.util.TracePlotter2D;

import javax.vecmath.Vector3f;

public class StaticOccupancyMap extends BayesFilter{

    private static Logger logger = Logger.getLogger(StaticOccupancyMap.class);

    public int worldRadius = 100; //radius of world, in meters

    //todo: initialize worldmap elements as .5 (all of them)
    float[][] worldMap = new float[200][200]; //creates a grid with 10cm resolution
    double[][] positionProbability = new double[200][200];



    Gnuplotter2 p = new Gnuplotter2(true);
    Gnuplotter2 map = new Gnuplotter2(true);
    Gnuplotter2 polar = new Gnuplotter2(true);

    //TracePlotter2D plot = new TracePlotter2D("Map", "x axis", "y axis");

    //Vector3f xAxis = new Vector3f(1, 0, 0);
    //Vector3f yAxis = new Vector3f(0, 1, 0);

    private HeatMap heatMap = new HeatMap();
    private HeatMap positionKalmanMap = new HeatMap();
    public KalmanXYThetaEstimate kalman = new KalmanXYThetaEstimate();
    //public EkfSlam slam = new EkfSlam();


    Matrix stateVector;
    Matrix uncertaintyCovariance;



    public void initialize(){
        //initializes settings required by the various plots
        p.setProperty("term", "x11");
        p.unsetProperty("key");
        p.setProperty("title", "'Distance to nearest object (0 = infinity or zero)'");
        p.setPlotParams("with points");

        map.setProperty("term", "x11");
        map.unsetProperty("key");
        map.setProperty("title", "'Map'");
        map.setPlotParams("pm3d");
        map.setPlotParams("palette", "grey");

        polar.setProperty("term", "x11");
        polar.unsetProperty("key");
        polar.setProperty("title", "'Polar representation of what the bee sees. +x direction is direction of travel.'");
        polar.setPlotParams("with points");

        kalman.initialize();

        //heatMap.initialize();
        positionKalmanMap.initialize();


        for (int i=0; i<200; i++){
            for (int j=0; j<200; j++){
                worldMap[i][j] = .5f;
          //      worldMap[99+xFromOrigin][99+yFromOrigin] = 1; //shifts coordinates so that they match up with the matrix
            }

        }
        /*
        map.clearData();
        for (int i=0; i<200; i++){
            for (int j=0; j<200; j++){

                if (worldMap[i][j] == 1){
                    map.addDataPoint(i + " " + j + " " + worldMap[i][j]); //plots

                    map.addDataPoint((currPos.x+99) + " " + (currPos.y+99) + " " + 2); //keeps track of bee location

                    //logger.info("beeTheta=" + beeTheta);
                }
            }
        }*/

        //map.writeLog("/home/markus/mapLogFile.txt");
        //map.splot();








    }

    public void drawRange(float range[]){

        p.clearData(); //clear previous plot
        String lineData = "";

        for (int h=0; h<181; h++){
            if (range[h] == Float.POSITIVE_INFINITY){
                range[h] = 0; //represent points that are out of measurement range as zero
            }
            lineData += (h+1)+ " " + range[h] + "\n "; //adds measured values to a string for plotting
        }
        p.addDataPoint(lineData); //plots h vs. range
        p.plot();

    }

    public void polarMap(float range[], float beeTheta){

        //Vector3f beePose = new Vector3f(beePosition.x, beePosition.y, beeTheta); //x,y,theta
        polar.clearData();
        int maxSensorRange = 20;

        for (int h=0; h<181; h++){
            float heading = beeTheta + 90 - h;  //gives angle from true north for each point in the beam
            double angle= Math.PI*heading/180.0; //converts to degrees

            if (range[h] == Float.POSITIVE_INFINITY){
                range[h] = maxSensorRange;  //for this visual representation, let points that are our of range be indicated as maximum
            }
            double xPoint = range[h]* Math.cos(angle); //find x,y coordinates corresponding to rangefinder measurements.
            double yPoint = range[h]* Math.sin(angle);
            String lineData = (xPoint+ " " + yPoint + "\n ");
            logger.info("x=" + xPoint + " y="+ yPoint + " heading=" + heading + " h=" +h);
            polar.addDataPoint(lineData);


        }
        polar.plot();

    }

    public float[][] scaledLaserHeading (float range[], float beeTheta){
        float[][] headingCoords = new float[181][2];

        for (int h=0; h<181; h++){
            float heading = beeTheta - 90 + h; //convert laser orientation to world-coordinate frame
            double angle = Math.PI*heading/180.0;

            if (range[h] == Float.POSITIVE_INFINITY){
                range[h] = 0; //or make max sensor range??
            }
            float xPoint = range[h]*(float)Math.cos(angle);
            float yPoint = range[h]*(float)Math.cos(angle);
            headingCoords[h][1] = xPoint;
            headingCoords[h][2] = yPoint;
        }

        return headingCoords;
    }

    public void occupancy (float range[], Vector3f currPos, float beeTheta){


        String mapData = "";
        for (int h=0; h<181; h++){
            float heading = beeTheta - 90 + h;  //gives angle from true north for each point in the beam
            double angle= Math.PI*heading/180.0;
            Vector3f rotatedLaser = new Vector3f((float)Math.cos(angle), (float)-Math.sin(angle), 0);
            //range [0] corresponds to 90 degrees, i.e. the left wing of the bee
            //range [180] corresponds to -90 degrees, i.e. the right wing of the bee

            if (range[h] == Float.POSITIVE_INFINITY){
                range[h] = 0;
            }
            rotatedLaser.scale(range[h]);
            //*double rotation [] = {Math.cos(angleFromX), Math.sin(angleFromX), 0};
            //double angle= Math.PI*heading/180.0;
            //float rotateZ[][] = {
            //        {(float)Math.cos(angle), (float)-Math.sin(angle), 0},
            //        {(float)Math.sin(angle), (float)Math.cos(angle), 0},
            //        {0, 0, 1}
            //};
            //float laserHeading[] = {0,0,0};
            //for (int i = 0; i<3; i++){
            //    for (int j = 0; j<3; j++){
            //        laserHeading[i] += rotateZ[i][j]*rotation[j];
            //    }
            //}
            //Vector3f rotatedLaser = new Vector3f(laserHeading[0], laserHeading[1], laserHeading[2]);
            //if (range[h] == Float.POSITIVE_INFINITY){
            //    range[h] = 0;
            //}
            //rotatedLaser.scale(range[h]);

            float xFromBee = rotatedLaser.x;
            float yFromBee = rotatedLaser.y;
            if (range[h] != 0){
                int xFromOrigin = Math.round((xFromBee + currPos.x)); //gives distance from origin
                int yFromOrigin = Math.round((yFromBee + currPos.y));

                //logger.info("range=" + range[h] + " heading=" + heading);
                //logger.info("xpoint = " + xFromOrigin + " ypoint = " + yFromOrigin + " xFromBee " + xFromBee + " yFromBee " + yFromBee);
                worldMap[499+xFromOrigin][499+yFromOrigin] = 1; //shifts coordinates so that they match up with the matrix

            }

        }
        map.clearData();
        for (int i=0; i<1000; i++){
            for (int j=0; j<1000; j++){
                if (worldMap[i][j] == 1){
                    map.addDataPoint(i + " " + j + " " + worldMap[i][j]); //plots
                    map.addDataPoint((currPos.x+499) + " " + (currPos.y+499) + " " + 2); //keeps track of bee location
                    //logger.info("beeTheta=" + beeTheta);
                }
            }
        }

        //map.writeLog("/home/markus/mapLogFile.txt");
        map.splot();

    }

    public void slamMap (float range[], Vector3f currPos, float beeTheta){

        boolean hallwayRight = false;
        boolean hallwayLeft = false;
        boolean wallAhead = false;
        Matrix motionVector = new Matrix(new double[][] {{0},{0},{0},{0},{0},{0}});
        for (int h=0; h<181; h++){
            if (range[h] == 0){
                range[h] = 20;
            }
        }
        if (range[0] > 12){
            hallwayLeft = true;
            logger.info("hallway on the left");
        }
        if (range[180] > 12){
            hallwayRight = true;
            logger.info("hallway on the right");
        }
        if (range[90] < 12){
            wallAhead = true;
            logger.info("wall ahead");
        }

        //slam.xythetaKalmanPredict(stateVector, motionVector, uncertaintyCovariance);
        kalman.predict(motionVector);

        if (wallAhead && (hallwayRight || hallwayLeft)){
            Matrix measurements = new Matrix(new double[][] {{currPos.x,currPos.y,beeTheta}});
            kalman.update(measurements);
            //slam.xythetaKalmanUpdate(stateVector, measurements, uncertaintyCovariance);
        }

        for (int i = 0; i<200; i++){
            for (int j = 0; j<200; j++){
                Matrix x = new Matrix(new double[][] {{i},{j},{0},{0},{0},{0}});
                stateVector = kalman.getStateVector();
                uncertaintyCovariance = kalman.getUncertaintyCovariance();
                Matrix xMinusState = x.minus(stateVector);
                if (uncertaintyCovariance.det() != 0){
                    double exponent = (xMinusState.transpose()).times((uncertaintyCovariance.inverse()).times(xMinusState)).get(0,0);
                    logger.info("exponent" + exponent);
                    positionProbability[i][j] = 1/(Math.sqrt(2*Math.PI)*uncertaintyCovariance.det());
                    positionProbability[i][j] *= Math.exp(-.5*exponent);
                    logger.info("probs: " + positionProbability[i][j] + " i" + i + " j" + j);
                }
            }
        }

        positionKalmanMap.setDataBlock(positionProbability, currPos);


    }

    public void bayesOccupancy (float range[], Vector3f currPos, float beeTheta) {
        BayesFilter bayesF = new BayesFilter();
        String mapData = "";
        for (int h=0; h<181; h++){
            float heading = beeTheta - 90 + h;  //gives angle from true north for each point in the beam
            double angle= Math.PI*heading/180.0;
            Vector3f rotatedLaser = new Vector3f((float)Math.cos(angle), (float)-Math.sin(angle), 0);
            if (range[h] == Float.POSITIVE_INFINITY){
                range[h] = 0;
            }
            rotatedLaser.scale(range[h]);
            float xFromBee = rotatedLaser.x;
            float yFromBee = rotatedLaser.y;
            if (range[h] != 0){
                int xFromOrigin = Math.round((xFromBee + currPos.x)); //gives distance from origin
                int yFromOrigin = Math.round((yFromBee + currPos.y));
                /*if (worldMap[499+xFromOrigin][499+yFromOrigin] == 0){

                    worldMap[499+xFromOrigin][499+yFromOrigin] = .5f;
                } */
                worldMap[99+xFromOrigin][99+yFromOrigin] = bayesF.update(worldMap[99 + xFromOrigin][99 + yFromOrigin]);

                logger.info("x="+xFromOrigin+" y="+ yFromOrigin+" prob=" + worldMap[99+xFromOrigin][99+yFromOrigin] + "h=" + h);


            }



        }
        for (int i=0; i<200; i++){
            for (int j=0; j<200; j++){
                double xDist = currPos.x - i + 99;
                double yDist = currPos.y - j + 99;


                double r = Math.sqrt(xDist*xDist + yDist*yDist);
                double phi = Math.atan2(yDist, -xDist);
                int phiDeg = (int)Math.round(phi*180/Math.PI);
                //logger.info("bee angle: " + beeTheta + " Phideg(before)=" + phiDeg + " i:" + i+ " j:" + j);

                if (phiDeg<0){
                    phiDeg += 360;
                }
                phiDeg = phiDeg + 90 - (int)beeTheta;
                //logger.info("Phideg(after)=" + phiDeg);
                //logger.info("angle to point: " + phiDeg + " beeTheta =" + (int)beeTheta);
                if (phiDeg >= 0 && phiDeg < 181) {
                    if (range[phiDeg] == 0){
                        range[phiDeg] = 20;
                    }
                    if (r<range[phiDeg] && worldMap[i][j]==0){  //the ==0 condition causes flickering

                        worldMap[i][j] = bayesF.downdate(worldMap[i][j]);
                        logger.info(" x= "+i+" y= "+j+" prob= " + worldMap[i][j] + "phiDeg" + phiDeg);

                    }
                    else if (r<range[phiDeg]){
                        worldMap[i][j] = bayesF.downdate(worldMap[i][j]);
                        logger.info(" x= "+i+" y= "+j+" prob= " + worldMap[i][j] + "this pixel has been updated." + " phiDeg=" + phiDeg);
                    }
                }
            }
        }
        /*map.clearData();

        for (int i=0; i<1000; i++){
            for (int j=0; j<1000; j++){
                if (worldMap[i][j] != 0){
                    map.addDataPoint(i + " " + j + " " + worldMap[i][j]); //plots
                    map.addDataPoint((currPos.x+499) + " " + (currPos.y+499) + " " + 2); //keeps track of bee location
                }
            }
        }
        map.splot();
        */

        heatMap.setDataBlock(worldMap, currPos);
    }


}