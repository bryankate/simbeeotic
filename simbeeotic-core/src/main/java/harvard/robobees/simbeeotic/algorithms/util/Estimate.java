package harvard.robobees.simbeeotic.algorithms.util;

import Jama.Matrix;



public interface Estimate {

   // public void xythetaKalmanPredict(Matrix stateVector, Matrix motionVector, Matrix covariance);
   // public void xythetaKalmanUpdate(Matrix stateVector, Matrix measurements, Matrix covariance);




    public void predict(Matrix motionVector);
    public void update(Matrix measurements);


}
