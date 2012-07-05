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


import Jama.Matrix;
import org.apache.log4j.Logger;

import java.util.Random;

/**
 * SLAM, implementing a particle filter and EKF.
 *
 * @author Mburkardt
 */


public class FastSlam implements FastSlamInterface{

    private static Logger logger = Logger.getLogger(FastSlam.class);
    public Matrix stateVector;
    public int numFeatures;
    public Matrix covariance;
    public Matrix controls;
    public Matrix measurements;
    public Random rand = new Random();


    //loop over all particles
    //sample pose
    //measurement likelihoods
    //measurement prediction
    //calculate jacobian

    //check if observed feature

   //initialize mean
    //calculate jacobian
    //initialize covariance
    //default importance weight

    //measurement prediction
    //calculate jacobian
    //measurement covariance
    //calculate kalman gain
    //update mean
    //update covariance
    //update weighting/importance factor

    //for unobserved features, leave everything unchanged

    //initialize new particle set
    //resample M particles

    @Override
    public void initialize(Matrix stateVector, int numFeatures, Matrix covariance, Matrix controls, Matrix measurements) {


    }

    //run for every particle:
    public void predict(Matrix stateVector, int numFeatures, Matrix covariance, Matrix controls, Matrix measurements){

        //stateVector is (x,y,theta,mean1,covariance1,...)

        double x = stateVector.get(0,0);
        double y = stateVector.get(1,0);
        double theta = stateVector.get(2,0);
        double vel = controls.get(0,0);
        double angVel = controls.get(2,0);
        double deltaTime = .1; //time interval between measurements

        //denoted as Q in table 7.2
        double sigmaR = .5;
        double sigmaPhi = .1;
        Matrix covarianceQ = new Matrix(new double[][] {{sigmaR, 0, 0},
                                                        {0, sigmaPhi, 0},
                                                        {0, 0, 0.5}});


        //sample new pose:
        double xCorrection = vel/angVel*(-Math.sin(theta) + Math.sin(theta+angVel*deltaTime));
        double yCorrection = vel/angVel*(Math.cos(theta) - Math.cos(theta + angVel * deltaTime));
        double thetaCorrection = angVel*deltaTime;
        stateVector.set(0,0, stateVector.get(0,0) + xCorrection);
        stateVector.set(1,0, stateVector.get(1,0) + yCorrection);
        stateVector.set(2,0, stateVector.get(2,0) + thetaCorrection);

        for (int k = 0; k<numFeatures; k++){

            //todo: verify this is where these values are supposed to come from.
            double rLandmark = measurements.get(2*k,0);
            double phiLandmark = measurements.get(2*k+1,0);

            double xLandmark = rLandmark*Math.cos(phiLandmark + stateVector.get(2,0));
            double yLandmark = rLandmark*Math.sin(phiLandmark + stateVector.get(2,0));
            //Matrix mean = h^-1(measure,stateVector.update)
            //jacobian of the measurement model with respect to the robot location, computed at predicted mean mu
            double q = Math.pow(xLandmark-x,2) + Math.pow(yLandmark-y, 2);
            Matrix jacobianH = new Matrix(3,3);
            jacobianH.set(0,0, -(xLandmark-x)/rLandmark);
            jacobianH.set(0,1, -(yLandmark-y)/rLandmark);
            jacobianH.set(1,0, (yLandmark-y)/q);
            jacobianH.set(1,1, -(xLandmark-x)/q);
            jacobianH.set(1,2, -1);
        }
    }

    public double getImporanceFactor(Matrix muMinusX, Matrix covariance){
        double importanceFactor = 0;
        if (covariance.det() != 0){
            double exponent = ((muMinusX.transpose()).times((covariance.inverse()).times(muMinusX))).get(0, 0);
            importanceFactor = Math.exp(-.5*exponent)/Math.sqrt(2*Math.PI*covariance.det());
        }
        return importanceFactor;
    }



    public void ekfInitialize(Matrix stateVector, Matrix covariance, Matrix controls, Matrix measurements){
        //based on ocw.mit.edu/courses/aeronautics-and-astronautics/16-412j-cognitive-robotics-spring-2005/projects/1aslam_blas_repo.pdf
        //Update current state using the odometry data.

        //stateVector is x,y,theta, landmarkX, landmarkY, ...
        //statevector is of dimension 3+2N x 1
        //covariance matrix is of dimension 3+2N x 3+2N
        //kalman gain is of dimension 3+2N x 2
        //measurements give x and y coords of landmarks

        double x = stateVector.get(0,0);
        double y = stateVector.get(1,0);
        double theta = stateVector.get(2,0);

        //control vector is in bee frame, with 0,0 being x-velocity, 1,0 being y-velocity, and 2,0 being angular velocity
        double vel = controls.get(0,0);
        double angVel = controls.get(2,0);
        double deltaTime = 1; //time interval between measurements
        double qErrorTerm = 0;

        //todo: for correction and jacobianA, double check the velocity thing
        double xCorrection = deltaTime*vel*Math.cos(theta)*(1+qErrorTerm);
        double yCorrection = deltaTime*vel*Math.sin(theta)*(1+qErrorTerm);
        double thetaCorrection = deltaTime*angVel*(1+qErrorTerm);

        //jacobian for the prediction model
        Matrix jacobianA = Matrix.identity(3,3);
        jacobianA.set(0,2, -deltaTime*vel*Math.sin(theta));
        jacobianA.set(1,2, deltaTime*vel*Math.cos(theta));

        //slam specific jacobians
        Matrix jacobianJxr = Matrix.identity(2,3);
        jacobianJxr.set(0,2, -deltaTime*vel*Math.sin(theta));
        jacobianJxr.set(1,2, deltaTime*vel*Math.cos(theta));

        Matrix jacobianJz = new Matrix(2,2);
        jacobianJz.set(0,0, Math.cos(theta + angVel*deltaTime));
        jacobianJz.set(0,1, -deltaTime*vel*Math.sin(theta + angVel*deltaTime));
        jacobianJz.set(1,0, Math.sin(theta + angVel*deltaTime));
        jacobianJz.set(1,1, deltaTime*vel*Math.cos(theta + angVel*deltaTime));

        //process noise
        Matrix c = new Matrix(new double[][]{{.1,0,0},{0,.1,0},{0,0,1}});
        Matrix w = new Matrix(new double[][]{{deltaTime*vel*Math.cos(theta)},
                                             {deltaTime*vel*Math.sin(theta)},
                                             {deltaTime*angVel}});
        Matrix processNoiseQ = w.times(c.times(w.transpose()));

        //measurement noise (R and V)
        //Matrix measurementNoise = new Matrix(new double[][] {{range*.1,0},{0,bearing*.1}});


        for (int i=0; i< measurements.getRowDimension(); i++){
            double xLandmark = measurements.get(2*i,0);
            double yLandmark = measurements.get(2*i+1,0);
            double range = Math.sqrt(Math.pow(xLandmark-x,2) + Math.pow(yLandmark-y,2));
            double bearing = Math.atan((yLandmark-y)/(xLandmark-x))-theta;

            //jacobian for the measurement model
            Matrix jacobianH = new Matrix(3,2);
            jacobianH.set(0,0, x-(xLandmark)/range);
            jacobianH.set(0,1, (y-yLandmark)/range);
            jacobianH.set(1,0, (yLandmark-y)/Math.pow(range,2));
            jacobianH.set(1,1, (xLandmark-x)/Math.pow(range,2));
            jacobianH.set(1,2, -1);

        }

    }

    public void ekfPredict(Matrix stateVector, Matrix controls, Matrix covariance){
        //based on ocw.mit.edu/courses/aeronautics-and-astronautics/16-412j-cognitive-robotics-spring-2005/projects/1aslam_blas_repo.pdf
        double x = stateVector.get(0,0);
        double y = stateVector.get(1,0);
        double theta = stateVector.get(2,0);

        //control vector is in bee frame, with 0,0 being x-velocity, 1,0 being y-velocity, and 2,0 being angular velocity
        double vel = controls.get(0,0);
        double angVel = controls.get(2,0);
        double deltaTime = 1; //time interval between measurements

        //todo: for correction and jacobianA, double check the velocity thing  (is deltaTime*vel what they call deltaT)
        double xCorrection = deltaTime*vel*Math.cos(theta);
        double yCorrection = deltaTime*vel*Math.sin(theta);
        double thetaCorrection = deltaTime*angVel;

        stateVector.set(0,0,x+xCorrection);
        stateVector.set(1,0,y+yCorrection);
        stateVector.set(2,0,theta+thetaCorrection);

        Matrix jacobianA = Matrix.identity(3,3);
        jacobianA.set(0,2, -deltaTime*vel*Math.sin(theta));
        jacobianA.set(1,2, deltaTime*vel*Math.cos(theta));

        double c = .1;
        Matrix processNoiseQ = new Matrix(new double[][] {
                {c*Math.pow(xCorrection,2), c*xCorrection*yCorrection, c*xCorrection*vel*deltaTime},
                {c*yCorrection*xCorrection, c*Math.pow(yCorrection,2), c*yCorrection*vel*deltaTime},
                {c*deltaTime*vel*xCorrection, c*deltaTime*vel*yCorrection, c*Math.pow(deltaTime*vel,2)}});

        Matrix positionCovariance = covariance.getMatrix(0,2,0,2);
        positionCovariance = (jacobianA.times(positionCovariance.times(jacobianA))).plus(processNoiseQ);

        for (int i = 0; i<3; i++){
            for (int j=0; j<3; j++){
                covariance.set(i,j,positionCovariance.get(i,j));
            }
        }

        Matrix positionCovarianceXCorr = covariance.getMatrix(0,2,0,covariance.getColumnDimension());
        positionCovarianceXCorr = jacobianA.times(positionCovarianceXCorr);

        for (int i=0; i<3; i++){
            for (int j=0; j<covariance.getColumnDimension(); j++){
                covariance.set(i,j,positionCovarianceXCorr.get(i,j));
            }
        }

    }

    public void ekfUpdate(Matrix stateVector, Matrix controls, Matrix covariance, Matrix measurements, int landmarkIndex){
        double x = stateVector.get(0,0);
        double y = stateVector.get(1,0);
        double theta = stateVector.get(2,0);
        double xLandmark = measurements.get(2*landmarkIndex,0); //let landmark index start at 0
        double yLandmark = measurements.get(2*landmarkIndex+1,0);
        double range = Math.sqrt(Math.pow(xLandmark-x,2) + Math.pow(yLandmark-y,2));
        //double bearing = Math.atan((yLandmark-y)/(xLandmark-x))-theta;
        Matrix noiseR = new Matrix(new double[][] {{.01*range,0},{0,.1}});

        double vel = controls.get(0,0);
        double deltaTime = 1;
        double xCorrection = deltaTime*vel*Math.cos(theta);
        double yCorrection = deltaTime*vel*Math.sin(theta);


        //jacobian for the measurement model
        Matrix jacobianH = new Matrix(2,covariance.getColumnDimension());
        jacobianH.set(0,0, (x-xLandmark)/range);
        jacobianH.set(0,1, (y-yLandmark)/range);
        jacobianH.set(1,0, (yLandmark-y)/Math.pow(range,2));
        jacobianH.set(1,1, (xLandmark-x)/Math.pow(range,2));
        jacobianH.set(1,2, -1);
        jacobianH.set(0,3+2*landmarkIndex, -(x-xLandmark)/range);
        jacobianH.set(0,4+2*landmarkIndex, -(y-yLandmark)/range);
        jacobianH.set(1,3+2*landmarkIndex, -(yLandmark-y)/Math.pow(range,2));
        jacobianH.set(1,4+2*landmarkIndex, -(xLandmark-x)/Math.pow(range,2));

        Matrix measurementsWithNoise = new Matrix(2,1);
        measurementsWithNoise.set(0,0, xLandmark * rand.nextGaussian());
        measurementsWithNoise.set(1,0, yLandmark * rand.nextGaussian());

        Matrix measurementPredict = new Matrix(2,1);
        measurementPredict.set(0,0, xLandmark + xCorrection);
        measurementPredict.set(1,0, yLandmark + yCorrection);

        Matrix innovationS = (jacobianH.times(covariance.times(jacobianH.transpose()))).plus(noiseR);
        Matrix kalmanGain = (controls.times(jacobianH.transpose())).times(innovationS.inverse());
        stateVector = stateVector.plus(kalmanGain.times(measurementsWithNoise.minus(measurementPredict)));
    }

    public void addLandmarks(Matrix stateVector, Matrix covariance, Matrix newLandmarks, Matrix controls){
        //based on ocw.mit.edu/courses/aeronautics-and-astronautics/16-412j-cognitive-robotics-spring-2005/projects/1aslam_blas_repo.pdf
        double x = stateVector.get(0,0);
        double y = stateVector.get(1,0);
        double xNewLandmark = newLandmarks.get(0,0);
        double yNewLandmark = newLandmarks.get(1,0);
        double theta = stateVector.get(2,0);
        double vel = controls.get(0,0);
        double angVel = controls.get(2,0);
        double deltaTime = .1;
        double range = Math.sqrt(Math.pow(xNewLandmark-x,2) + Math.pow(yNewLandmark-y,2));
        Matrix noiseR = new Matrix(new double[][] {{.01*range,0},{0,.1}});
        Matrix jacobianJxr = Matrix.identity(2,3);
        jacobianJxr.set(0,2, -deltaTime*vel*Math.sin(theta));
        jacobianJxr.set(1,2, deltaTime*vel*Math.cos(theta));

        Matrix jacobianJz = new Matrix(2,2);
        jacobianJz.set(0,0, Math.cos(theta + angVel*deltaTime));
        jacobianJz.set(0,1, -deltaTime*vel*Math.sin(theta + angVel*deltaTime));
        jacobianJz.set(1,0, Math.sin(theta + angVel*deltaTime));
        jacobianJz.set(1,1, deltaTime*vel*Math.cos(theta + angVel*deltaTime));

        Matrix updatedStateVector = new Matrix(stateVector.getRowDimension(), 1);
        for (int i=0; i<stateVector.getRowDimension(); i++){
            updatedStateVector.set(i, 0, stateVector.get(i,0));
        }
        updatedStateVector.set(stateVector.getRowDimension(),0, xNewLandmark);
        updatedStateVector.set(stateVector.getRowDimension() + 1, 0, yNewLandmark);

        Matrix updatedCovariance = new Matrix(covariance.getRowDimension()+2, covariance.getColumnDimension()+2);
        for (int i=0; i<covariance.getRowDimension(); i++){
            for (int j=0; j<covariance.getColumnDimension(); j++){
                updatedCovariance.set(i,j, covariance.get(i,j));
            }
        }

        //todo: I'm not sure if these next two lines are correct
        Matrix robotRobotCovariance = covariance.getMatrix(0,2,0,2);
        Matrix newLandmarkCovariance = jacobianJxr.times(robotRobotCovariance.times(jacobianJxr.transpose()));
        newLandmarkCovariance = newLandmarkCovariance.plus(jacobianJz.times(noiseR.times(jacobianJz.transpose()))); //p^N+1,N+1
        for (int i=covariance.getRowDimension()-2; i<covariance.getRowDimension(); i++){
            for (int j=covariance.getColumnDimension()-2; j<covariance.getColumnDimension(); j++){
                updatedCovariance.set(i,j, newLandmarkCovariance.get(i,j));
            }
        }

        Matrix robotLandmarkCovariance = robotRobotCovariance.times(jacobianJxr.transpose()); //p^r,N+1
        for (int i=covariance.getRowDimension(); i<updatedCovariance.getRowDimension(); i++){
            for (int j=covariance.getColumnDimension(); j<updatedCovariance.getColumnDimension(); j++){
                updatedCovariance.set(i,j, robotLandmarkCovariance.get(i-covariance.getRowDimension(),j-covariance.getColumnDimension()));
                updatedCovariance.set(j,i, robotLandmarkCovariance.get(i-covariance.getRowDimension(),j-covariance.getColumnDimension()));
            }
        }

        Matrix positionCovarianceXCorr = covariance.getMatrix(0,2,3,covariance.getColumnDimension()-1); //p^ri
        Matrix landmarkLandmarkCovariance = jacobianJxr.times(positionCovarianceXCorr.transpose());
        for (int i=covariance.getRowDimension(); i<updatedCovariance.getRowDimension(); i++){
            for (int j=3; j<covariance.getColumnDimension(); j++){
                updatedCovariance.set(i,j, landmarkLandmarkCovariance.get(i-covariance.getRowDimension(),j-3));
                updatedCovariance.set(j,i, landmarkLandmarkCovariance.get(i-covariance.getRowDimension(),j-3));
            }
        }


    }


    public Matrix getCovariance() {
        return covariance;
    }

    public void setCovariance(Matrix covariance) {
        this.covariance = covariance;
    }

    public Matrix getStateVector() {
        return stateVector;
    }

    public void setStateVector(Matrix stateVector) {
        this.stateVector = stateVector;
    }


}


