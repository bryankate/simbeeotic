package harvard.robobees.simbeeotic.model.sensor;


import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public interface Gyroscope {

    /**
     * Gets the angular velocity of the body relative to three axes. The
     * vector returned is (xAngVel, yAngVel, zAngVel) = (roll rate, pitch rate, yaw rate)
     * in rad/s.
     *
     * @return The angular rates about the X, Y, and Z axes in the body frame.
     */
    public Vector3f getAngularVelocity();
}
