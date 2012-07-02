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

/**
 * A simple Kalman filter for x,y,theta, x_dot, y_dot, and theta_dot.
 * @author Mburkardt
 */

public class KalmanXYThetaEstimate  implements Estimate{

    Matrix stateVector;
    Matrix uncertaintyCovariance;

    public void initialize(){
        stateVector = new Matrix(new double[][] {{4}, {12}, {20}, {0}, {0}, {0}});
        uncertaintyCovariance = new Matrix(new double[][] {{0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,0,0,0},
                {0,0,0,1000,0,0},
                {0,0,0,0,1000,0},
                {0,0,0,0,0,1000}});
        //stateVector = new Matrix(6,1);
        //uncertaintyCovariance = new Matrix(6,6);
    }

    @Override
    public void predict(Matrix motionVector) {

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

    @Override
    public void update(Matrix measurements) {
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


    public Matrix getUncertaintyCovariance() {
        return uncertaintyCovariance;
    }

    public void setUncertaintyCovariance(Matrix uncertaintyCovariance) {
        this.uncertaintyCovariance = uncertaintyCovariance;
    }

    public Matrix getStateVector() {
        return stateVector;
    }

    public void setStateVector(Matrix stateVector) {
        this.stateVector = stateVector;
    }
}
