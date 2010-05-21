package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.model.sensor.Accelerometer;
import harvard.robobees.simbeeotic.model.sensor.Compass;
import harvard.robobees.simbeeotic.model.sensor.ContactSensor;
import harvard.robobees.simbeeotic.model.sensor.Gyroscope;
import harvard.robobees.simbeeotic.model.sensor.RangeSensor;
import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class TestingBee extends SimpleBee {

    private Accelerometer accelerometer;
    private Gyroscope gyro;
    private Compass compass;
    private RangeSensor rangeBottom;
    private ContactSensor contactBottom;


    @Override
    public void initialize() {

        super.initialize();

        accelerometer = getSensor("accelerometer", Accelerometer.class);
        gyro = getSensor("gyro", Gyroscope.class);
        compass = getSensor("compass", Compass.class);
        rangeBottom = getSensor("rangeBottom", RangeSensor.class);
        contactBottom = getSensor("contactBottom", ContactSensor.class);
    }


    @Override
    protected void updateKinematics(SimTime time) {

        Vector3f pos = getTruthPosition();
        Vector3f linVel = getTruthLinearVelocity();
        Vector3f angVel = getTruthAngularVelocity();
        Vector3f linAccel = getTruthLinearAcceleration();
        Vector3f angAccel = getTruthAngularAcceleration();
        Vector3f orient = MathUtil.quaternionToEulerZYX(getTruthOrientation());

        float heading = compass.getHeading();
        Vector3f accelSens = accelerometer.getLinearAcceleration();
        Vector3f gyroSens = gyro.getAngularVelocity();
        float rangeSens = rangeBottom.getRange();
        boolean contactSens = contactBottom.isTripped();

        System.out.println("ID: " + getModelId() + "  " +
                           "time: " + time + "  " +
                           "pos: " + pos.x + " " + pos.y + " " + pos.z + "  " +
                           "linVel: " + linVel.x + " " + linVel.y + " " + linVel.z + "  " +
                           "angVel: " + angVel.x + " " + angVel.y + " " + angVel.z + "  " +
                           "linAccel: " + linAccel.x + " " + linAccel.y + " " + linAccel.z + "  " +
                           "angAccel: " + angAccel.x + " " + angAccel.y + " " + angAccel.z + "  " +
                           "orient: " + orient.x + " " + orient.y + " " + orient.z + "  " +
                           "accelerometer: " + accelSens.x + " " + accelSens.y + " " + accelSens.z + "  " +
                           "gyro: " + gyroSens.x + " " + gyroSens.y + " " + gyroSens.z + "  " +
                           "heading: " + heading + "  " +
                           "range: " + rangeSens + "  " +
                           "contact: " + contactSens + "  " +
                           "active: " + isActive());
    }


    @Override
    public void finish() {
    }
}
