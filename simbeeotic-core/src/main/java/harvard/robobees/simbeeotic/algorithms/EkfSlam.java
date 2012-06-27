package harvard.robobees.simbeeotic.algorithms;

import Jama.Matrix;
import org.apache.log4j.Logger;
import java.util.Random;

public class EkfSlam {

    private Random rand;

    private static Logger logger = Logger.getLogger(EkfSlam.class);

    //todo: does a matrix index start counting at 0 or at 1?
    //todo: make us of sparse matrix multiplication?

//    int numLandmarks;
//    int stateVectorDim = 3*numLandmarks + 3;
    //state vector: (x,y,theta,landmark_1,x,landmark_1,y,landmark_1,signature,...)
    //initial pose is taken to be the origin-->get true location, then for any location afterwards, subtract
    //to get the location with respect to "origin"


//    Matrix mu_not = new Matrix(stateVectorDim,1);
//    Matrix sigma_not = new Matrix(stateVectorDim,stateVectorDim);
    //Matrix stateVector = new Matrix(stateVectorDim,1);
//    double deltaTime = .1;



//    public void initialize(){
//        for (int i = 2; i<(3*numLandmarks + 3); i++){
//            sigma_not.set(i,i,Float.POSITIVE_INFINITY);
//       }
//    }

    public Random getRandom(){
        if (rand==null){
            rand = new Random();
        }
        return rand;
    }

    public void simpleKalman (){

        //define:
        //state vector x
        Matrix stateVector = new Matrix(new double[][] {{-4}, {8}, {0}, {0}});
        //measurements (u)
        Matrix measurements = new Matrix(new double[][] {{1,4},{6,0},{11,-4},{16,-8}});
        //Matrix initialXY = new Matrix(new double[][] {{-4,8}});

        //u:
        Matrix motionVector = new Matrix(new double[][] {{0},{0},{0},{0}});
        //P:
        Matrix uncertaintyCovariance = new Matrix(new double[][] {{0,0,0,0},
                                                                 {0,0,0,0},
                                                                 {0,0,1000,0},
                                                                 {0,0,0,1000}});
        //F:
        Matrix stateTransitionMatrix = new Matrix(new double [][] {{1, 0, .1, 0},
                                                                   {0,1,0,.1},
                                                                   {0,0,1,0},
                                                                   {0,0,0,1}});
        //H:
        Matrix measurementFunctionProjection = Matrix.identity(2,4);
        //R:
        Matrix measurementNoise = Matrix.identity(2,2).times(.1);
        //I:
        Matrix eye4 = Matrix.identity(4,4);


        for (int n = 0; n<measurements.getRowDimension(); n++){
            //prediction:
            stateVector = stateTransitionMatrix.times(stateVector).plus(motionVector);
            uncertaintyCovariance = stateTransitionMatrix.times(uncertaintyCovariance.times(stateTransitionMatrix.transpose()));
            //measurement update:
            Matrix z = new Matrix(new double[][] {{measurements.get(n,0)},{measurements.get(n,1)}});
            Matrix y = z.minus(measurementFunctionProjection.times(stateVector));
            Matrix s = measurementFunctionProjection.times(uncertaintyCovariance.times(measurementFunctionProjection.transpose()));
            s = s.plus(measurementNoise);
            Matrix sInverse = s.inverse();
            Matrix kalmanGain = uncertaintyCovariance.times(measurementFunctionProjection.transpose());
            kalmanGain = kalmanGain.times(sInverse);
            stateVector = stateVector.plus(kalmanGain.times(y));
            uncertaintyCovariance = eye4.minus(kalmanGain.times(measurementFunctionProjection)).times(uncertaintyCovariance);
            logger.info("state vector" + stateVector.get(0,0) + " " + stateVector.get(1,0) + " " + stateVector.get(2,0)
            + stateVector.get(3,0));
        }
    }

    public void xythetaKalmanPredict (Matrix stateVector, Matrix motionVector, Matrix uncertaintyCovariance){
        double deltaT = .1;
        Matrix stateTransitionMatrix = new Matrix(new double [][] {{1, 0, 0, deltaT, 0, 0},
                                                                  {0,1,0, 0,deltaT, 0},
                                                                  {0,0,1,0, 0, deltaT},
                                                                  {0,0,0,1,0,0},
                                                                  {0,0,0,0,1,0},
                                                                  {0,0,0,0,0,1}});
        stateVector = stateTransitionMatrix.times(stateVector).plus(motionVector);
        uncertaintyCovariance = stateTransitionMatrix.times(uncertaintyCovariance.times(stateTransitionMatrix.transpose()));

    }

    public void xythetaKalmanUpdate (Matrix stateVector, Matrix measurements, Matrix uncertaintyCovariance){
        double deltaT = .1;
        Matrix stateTransitionMatrix = new Matrix(new double [][] {{1, 0, 0, deltaT, 0, 0},
                {0,1,0, 0,deltaT, 0},
                {0,0,1,0, 0, deltaT},
                {0,0,0,1,0,0},
                {0,0,0,0,1,0},
                {0,0,0,0,0,1}});
        //H:
        Matrix measurementFunctionProjection = Matrix.identity(3,6);
        //R:
        Matrix measurementNoise = Matrix.identity(3,3).times(.1);
        //I:
        Matrix eye4 = Matrix.identity(6,6);

        Matrix z = new Matrix(new double[][] {{measurements.get(0,0)},{measurements.get(0,1)}, {measurements.get(0,2)}});
        Matrix y = z.minus(measurementFunctionProjection.times(stateVector));
        Matrix s = measurementFunctionProjection.times(uncertaintyCovariance.times(measurementFunctionProjection.transpose()));
        s = s.plus(measurementNoise);
        Matrix sInverse = s.inverse();
        Matrix kalmanGain = uncertaintyCovariance.times(measurementFunctionProjection.transpose());
        kalmanGain = kalmanGain.times(sInverse);
        stateVector = stateVector.plus(kalmanGain.times(y));
        uncertaintyCovariance = eye4.minus(kalmanGain.times(measurementFunctionProjection)).times(uncertaintyCovariance);

    }

    public void xythetaKalman (){

        double[][] positionProbs = new double[10][10];
        //define:
        //state vector x
        //initial values for (x,y,theta,x-dot,y-dot,theta-dot)
        Matrix stateVector = new Matrix(new double[][] {{4}, {12}, {0}, {0}});
        //measurements (u)
        Matrix measurements = new Matrix(new double[][] {{5,10},{6,8},{7,6},{8,4},{9,2},{10,0}});
        //u:
        Matrix motionVector = new Matrix(new double[][] {{0},{0},{0},{0}});
        //P:
        Matrix uncertaintyCovariance = new Matrix(new double[][] {{0,0,0,0},
                                                                 {0,0,0,0},
                                                                 {0,0,1000,0},
                                                                 {0,0,0,1000}});

        //F:
        double deltaT = .1;
        Matrix stateTransitionMatrix = new Matrix(new double [][] {{1, 0, deltaT, 0},
                                                                  {0,1,0, deltaT},
                                                                  {0,0,1,0},
                                                                  {0,0,0,1}});
        //H:
        Matrix measurementFunctionProjection = Matrix.identity(2,4);
        //R:
        Matrix measurementNoise = Matrix.identity(2,2).times(.1);
        //I:
        Matrix eye4 = Matrix.identity(4,4);


        for (int n = 0; n<measurements.getRowDimension(); n++){
            //prediction:
            stateVector = stateTransitionMatrix.times(stateVector).plus(motionVector);
            uncertaintyCovariance = stateTransitionMatrix.times(uncertaintyCovariance.times(stateTransitionMatrix.transpose()));
            //measurement update:
            Matrix z = new Matrix(new double[][] {{measurements.get(n,0)},{measurements.get(n,1)}});
            Matrix y = z.minus(measurementFunctionProjection.times(stateVector));
            Matrix s = measurementFunctionProjection.times(uncertaintyCovariance.times(measurementFunctionProjection.transpose()));
            s = s.plus(measurementNoise);
            Matrix sInverse = s.inverse();
            Matrix kalmanGain = uncertaintyCovariance.times(measurementFunctionProjection.transpose());
            kalmanGain = kalmanGain.times(sInverse);
            stateVector = stateVector.plus(kalmanGain.times(y));
            uncertaintyCovariance = eye4.minus(kalmanGain.times(measurementFunctionProjection)).times(uncertaintyCovariance);
            logger.info("state vector" + stateVector.get(0,0) + " " + stateVector.get(1,0) + " " + stateVector.get(2,0)
                    + " " + stateVector.get(3,0));

            for (int i = 0; i<10; i++){
                for (int j = 0; j<10; j++){
                    //Matrix x = new Matrix(new double[][] {{i},{j},{stateVector.get(2,0)},{stateVector.get(3,0)}});
                    Matrix x = new Matrix(new double[][] {{stateVector.get(0,0)+.01},{stateVector.get(1,0)+.01},{stateVector.get(2,0)-.01},{stateVector.get(3,0)-.01}});
                    Matrix xMinusState = x.minus(stateVector);
                    if (uncertaintyCovariance.det() != 0){
                        double exponent = (xMinusState.transpose()).times((uncertaintyCovariance.inverse()).times(xMinusState)).get(0,0);
                        logger.info("exponent" + exponent + " determinant" + uncertaintyCovariance.det());
                        positionProbs[i][j] = 1/(Math.sqrt(2*Math.PI*uncertaintyCovariance.det()));
                        positionProbs[i][j] *= Math.exp(-.5*exponent);
                        logger.info("probs: " + positionProbs[i][j] + " i" + i + " j" + j);
                    }
                }
            }


        }


    }

    public void kalmanDebug (){

        double[][] positionProbs = new double[10][10];
        //define:
        //state vector x
        //initial values for (x,y,theta,x-dot,y-dot,theta-dot)
        Matrix stateVector = new Matrix(new double[][] {{5}, {5}, {1}, {1}});
        //measurements (u)

        //u:

        //P:
        Matrix uncertaintyCovariance = new Matrix(new double[][] {{2,0,.1,0},
                                                                  {0,2,0,.1},
                                                                  {.1,0,.5,0},
                                                                  {0,.1,0,.5}});


            for (int i = 0; i<10; i++){
                for (int j = 0; j<10; j++){
                    //Matrix x = new Matrix(new double[][] {{i},{j},{stateVector.get(2,0)},{stateVector.get(3,0)}});
                    Matrix x = new Matrix(new double[][] {{i},{j},{1},{1}});
                    Matrix xMinusState = x.minus(stateVector);
                    if (uncertaintyCovariance.det() != 0){
                        double exponent = (xMinusState.transpose()).times((uncertaintyCovariance.inverse()).times(xMinusState)).get(0,0);
                        //logger.info("exponent" + exponent + " determinant" + uncertaintyCovariance.det());
                        positionProbs[i][j] = Math.pow(2*Math.PI,-2)/Math.sqrt(uncertaintyCovariance.det());
                        positionProbs[i][j] *= Math.exp(-.5*exponent);
                        logger.info("debug_probs: " + positionProbs[i][j] + " i" + i + " j" + j);
                    }
                }
            }


        }





    public void ekfLocalization(Matrix stateVector, Matrix covariance, Matrix controls, Matrix measurements){

        /* My inputs:
        stateVector = new Matrix(new double[][] {{0}, {0}, {0}});
        Matrix covariance = new Matrix(new double[][] {{1,0,0},
                                                       {0,1,0},
                                                       {0,0,.1}});

        Matrix controls = new Matrix(new double[][] {{0}, {0}, {Math.PI/4}});
        Matrix measurements = new Matrix(new double[][] {{10.23},{.61},{29.81}, {-.96}});
         */

        /*
        when initializing: - X:0.0 Y:0.0 heading:0.0
        after rotation - X:0.0 Y:0.0 heading:0.7853981633974483
        first update - X:-0.10573713162939963 Y:-0.13714319707683537 heading:0.8716848927729379
        second update - X:0.04566771301886724 Y:-0.1426516622075272 heading:0.9652610614077519

         */


        logger.info("X:" + stateVector.get(0,0) + " Y:" + stateVector.get(1,0) + " heading:" + stateVector.get(2,0));

        //stateVector is x,y,theta, landmarkX, landmarkY, landmarkIndex
        //line 2
        double x = stateVector.get(0,0);
        double y = stateVector.get(1,0);
        double theta = stateVector.get(2,0);

        //control vector is in bee frame, with 0,0 being x-velocity, 1,0 being y-velocity, and 2,0 being angular velocity
        double vel = controls.get(0,0);
        double angVel = controls.get(2,0);
        double deltaTime = 1; //time interval between measurements

        //line 3
        Matrix jacobianG = Matrix.identity(3,3); //derivative of g with respect to x_t-1, evaluated at u and mu_t-1.
        jacobianG.set(0,2,vel/angVel*(-Math.cos(theta) + Math.cos(theta+angVel*deltaTime)));
        jacobianG.set(1,2,vel/angVel*(-Math.sin(theta)+Math.sin(theta+angVel*deltaTime)));

        //line 4
        Matrix jacobianV = new Matrix(3,2); //used for transformation from control space to state space
        //derivative of motion function g with respect to motion parameters, evaulated at u(t) and mu(t-1).
        jacobianV.set(0,0,(-Math.sin(theta)+Math.sin(theta+angVel*deltaTime))/angVel);
        jacobianV.set(1,0,(Math.cos(theta)-Math.cos(theta + angVel * deltaTime))/angVel);
        jacobianV.set(0,1, (vel*(Math.sin(theta)-Math.sin(theta+angVel*deltaTime)))/Math.pow(angVel,2)+(vel*Math.cos(theta+angVel*deltaTime)*deltaTime)/angVel);
        jacobianV.set(1,1, (-vel*(Math.cos(theta)-Math.cos(theta + angVel * deltaTime)))/Math.pow(angVel,2)+(vel*Math.sin(theta + angVel * deltaTime)*deltaTime)/angVel);
        jacobianV.set(2,1,deltaTime);

        double alpha1 = .1; //elements required for covariance matrix
        double alpha2 = .01; //todo: figure out what they actually mean. also, why are there only 3??
        double alpha3 = .1;
        double alpha4 = .01;

        //line 5
        //covariance matrix of the noise in control space
        //denoted as M in table 7.2 of probabilistic robotics
        Matrix controlSpaceNoiseCovariance = new Matrix(2,2);
        controlSpaceNoiseCovariance.set(0,0, alpha1*Math.pow(vel,2) + alpha2*Math.pow(angVel,2));
        controlSpaceNoiseCovariance.set(1,1, alpha3*Math.pow(vel,2) + alpha4*Math.pow(angVel,2));

        //line 6
        //update statevector(mean)
        double xCorrection = vel/angVel*(-Math.sin(theta) + Math.sin(theta+angVel*deltaTime));
        double yCorrection = vel/angVel*(Math.cos(theta) - Math.cos(theta + angVel * deltaTime));
        double thetaCorrection = angVel*deltaTime;

        stateVector.set(0,0, stateVector.get(0,0) + xCorrection);
        stateVector.set(1,0, stateVector.get(1,0) + yCorrection);
        stateVector.set(2,0, stateVector.get(2,0) + thetaCorrection);
        logger.info("X:" + stateVector.get(0,0) + " Y:" + stateVector.get(1,0) + " heading:" + stateVector.get(2,0));
        //line 7
        //update covariance
        covariance = jacobianG.times(covariance.times(jacobianG.transpose())).plus(jacobianV.times(controlSpaceNoiseCovariance.times(jacobianV.transpose())));

        //line 8
        //measurement noise
        //denoted as Q in table 7.2
        Matrix measurementNoise = new Matrix(3,3);
        double sigmaR = .5;
        double sigmaPhi = .1;
        measurementNoise.set(0,0,sigmaR);
        measurementNoise.set(1,1,sigmaPhi); //.1 is a placeholder for phi noise
        measurementNoise.set(2,2,.05);//.05 is a placeholder for s(landmark signature) noise

        //line 9
        for (int n = 0; n<2; n++){

            double rLandmark = measurements.get(2*n,0);
            double phiLandmark = measurements.get(2*n+1,0);

            double xLandmark = rLandmark*Math.cos(phiLandmark + stateVector.get(2,0));
            double yLandmark = rLandmark*Math.sin(phiLandmark + stateVector.get(2,0));

            //double xLandmark = stateVector.get(3*n+3, 0);
            //double yLandmark = stateVector.get(3*n+4, 0);
            double signatureLandmark = 1;

            //double rLandmark = Math.sqrt(Math.pow(xLandmark-x,2) + Math.pow(yLandmark-y, 2));
            //double phiLandmark = Math.atan2(yLandmark-y, xLandmark-x)-theta;

            //line 11
            double q = Math.pow(xLandmark-x,2) + Math.pow(yLandmark-y, 2);

            //line 12
            Matrix prediction = new Matrix(new double[][] {{10, stateVector.get(2,0)}, {30, stateVector.get(2,0)-Math.PI/2}});
            Matrix measurementPredict = new Matrix(new double[][] {{prediction.get(n,0)},{prediction.get(n,1)},{signatureLandmark}});

            //double rNoise = rand.nextGaussian()*sigmaR;
            //double phiNoise = rand.nextGaussian()*sigmaPhi;

            //Matrix noise = new Matrix(new double[][] {{rNoise}, {phiNoise}, {0}});
            //Matrix measurementWithNoise = measurementPredict.plus(noise);
            Matrix measurementWithNoise = new Matrix(new double[][] {{rLandmark},{phiLandmark},{signatureLandmark}});;

            //jacobian of h with respect to the robot location, computed at predicted mean mu
            Matrix jacobianH = new Matrix(3,3);
            jacobianH.set(0,0, -(xLandmark-x)/rLandmark);
            jacobianH.set(0,1, -(yLandmark-y)/rLandmark);
            jacobianH.set(1,0, (yLandmark-y)/q);
            jacobianH.set(1,1, -(xLandmark-x)/q);
            jacobianH.set(1,2, -1);

            //line 14
            //determines uncertainty corresponding to predicted measurement
            Matrix uncertaintyS = jacobianH.times(covariance.times(jacobianH.transpose())).plus(measurementNoise);

            //line 15
            //kalman gain
            Matrix kalmanGain = covariance.times((jacobianH.transpose()).times(uncertaintyS.inverse()));

            //line 16
            stateVector = stateVector.plus(kalmanGain.times(measurementWithNoise.minus(measurementPredict)));

            //line 17
            Matrix eye3 = Matrix.identity(3,3);
            covariance = (eye3.minus(kalmanGain.times(jacobianH))).times(covariance);
            logger.info("X:" + stateVector.get(0,0) + " Y:" + stateVector.get(1,0) + " heading:" + stateVector.get(2,0));
        }





    }

    public void SLAM_KnownCorrespondences (double heading, double velocity, double angVel){
  /*      //the update to the statevector is the motion function g(u_t,y_t-1)
        //y_t-1 is the previous state vector

    //line 2:
        Matrix fSubX = Matrix.identity(3,stateVectorDim);

    //line 3:
        double updateForX = -velocity/angVel *Math.sin(heading) + velocity/angVel*Math.sin(heading + angVel*deltaTime);
        double updateForY = velocity/angVel*Math.cos(heading)-velocity/angVel*Math.cos(heading+angVel*deltaTime);
        double updateForTheta = angVel*deltaTime;
        stateVector.set(0,0, stateVector.get(0,0) + updateForX);
        stateVector.set(1,0, stateVector.get(1,0) + updateForY);
        stateVector.set(2,0, stateVector.get(2,0) + updateForTheta);
        //todo: add that error matrix? (eq10.13)

    //line 4:
        double muPrevTheta = mu_not.get(2,0);
        Matrix littleGSubT = new Matrix(3,3);
        littleGSubT.set(0,2, -velocity/angVel*Math.cos(muPrevTheta) + velocity/angVel*Math.cos(muPrevTheta+angVel*deltaTime));
        littleGSubT.set(1,2, -velocity/angVel*Math.sin(muPrevTheta) + velocity/angVel*Math.sin(muPrevTheta + angVel * deltaTime));

        Matrix bigGSubT = Matrix.identity(stateVectorDim, stateVectorDim).plus(fSubX.transpose().times(littleGSubT).times(fSubX));



        double landmarkX = stateVector.get(1+3*jLandmarkIndex,0);
        double landmarkY = stateVector.get(2+3*jLandmarkIndex,0);
        double xPose = stateVector.get(0,0);
        double yPose = stateVector.get(1,0);
        double thetaPose = stateVector.get(2,0);

        Matrix zSubTSuperI = new Matrix(3,1);
        zSubTSuperI.set(0,0, Math.sqrt(Math.pow(landmarkX-xPose,2) + Math.pow(landmarkY-yPose,2)));
        zSubTSuperI.set(1,0, Math.atan2(landmarkY-yPose, landmarkX-xPose) - thetaPose);
        zSubTSuperI.set(2,0, jLandmarkIndex);
        //todo: add that error matrix (eq10.17);pp

    }
   */


}


}