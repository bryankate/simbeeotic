package harvard.robobees.simbeeotic.algorithms;

import Jama.Matrix;



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
