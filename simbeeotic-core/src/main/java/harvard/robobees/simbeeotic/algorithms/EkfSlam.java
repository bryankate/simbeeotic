package harvard.robobees.simbeeotic.algorithms;

import Jama.Matrix;
import org.apache.log4j.Logger;

public class EkfSlam {

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


    public void xythetaKalman (Matrix stateVector, Matrix measurements, Matrix motionVector, Matrix uncertaintyCovariance){


        //define:
        //state vector x
        //initial values for (x,y,theta,x-dot,y-dot,theta-dot)
        //Matrix stateVector = new Matrix(new double[][] {{4}, {12}, {20}, {0}, {0}, {0}});
        //measurements (u)
        //Matrix measurements = new Matrix(new double[][] {{5,10,30},{6,8,40},{7,6,50},{8,4,60},{9,2,70},{10,0,80}});
        //u:
        //Matrix motionVector = new Matrix(new double[][] {{0},{0},{0},{0},{0},{0}});
        //P:
        /*Matrix uncertaintyCovariance = new Matrix(new double[][] {{0,0,0,0,0,0},
                                                                 {0,0,0,0,0,0},
                                                                 {0,0,0,0,0,0},
                                                                 {0,0,0,1000,0,0},
                                                                 {0,0,0,0,1000,0},
                                                                 {0,0,0,0,0,1000}});
        */
        //F:
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


        for (int n = 0; n<measurements.getRowDimension(); n++){
            //prediction:
            stateVector = stateTransitionMatrix.times(stateVector).plus(motionVector);
            uncertaintyCovariance = stateTransitionMatrix.times(uncertaintyCovariance.times(stateTransitionMatrix.transpose()));
            //measurement update:
            Matrix z = new Matrix(new double[][] {{measurements.get(n,0)},{measurements.get(n,1)},{measurements.get(n,2)}});
            Matrix y = z.minus(measurementFunctionProjection.times(stateVector));
            Matrix s = measurementFunctionProjection.times(uncertaintyCovariance.times(measurementFunctionProjection.transpose()));
            s = s.plus(measurementNoise);
            Matrix sInverse = s.inverse();
            Matrix kalmanGain = uncertaintyCovariance.times(measurementFunctionProjection.transpose());
            kalmanGain = kalmanGain.times(sInverse);
            stateVector = stateVector.plus(kalmanGain.times(y));
            uncertaintyCovariance = eye4.minus(kalmanGain.times(measurementFunctionProjection)).times(uncertaintyCovariance);
            logger.info("state vector" + stateVector.get(0,0) + " " + stateVector.get(1,0) + " " + stateVector.get(2,0)
                    + " " + stateVector.get(3,0) + " " + stateVector.get(4,0) + " " + stateVector.get(5,0));
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