package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.model.sensor.Accelerometer;
import harvard.robobees.simbeeotic.model.sensor.Compass;
import harvard.robobees.simbeeotic.model.sensor.ContactSensor;
import harvard.robobees.simbeeotic.model.sensor.Gyroscope;
import harvard.robobees.simbeeotic.model.sensor.RangeSensor;
import harvard.robobees.simbeeotic.util.MathUtil;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class TestingBee implements GenericBeeLogic {

    private GenericBee host;

    private Accelerometer accelerometer;
    private Gyroscope gyro;
    private Compass compass;
    private RangeSensor rangeBottom;
    private ContactSensor contactBottom;


    @Override
    public void initialize(GenericBee bee) {

        host = bee;

        accelerometer = host.getSensor("accelerometer", Accelerometer.class);
        gyro = host.getSensor("gyro", Gyroscope.class);
        compass = host.getSensor("compass", Compass.class);
        rangeBottom = host.getSensor("rangeBottom", RangeSensor.class);
        contactBottom = host.getSensor("contactBottom", ContactSensor.class);
    }


    @Override
    public void update(double time) {

        Vector3f pos = host.getTruthPosition();
        Vector3f linVel = host.getTruthLinearVelocity();
        Vector3f angVel = host.getTruthAngularVelocity();
        Vector3f linAccel = host.getTruthLinearAcceleration();
        Vector3f angAccel = host.getTruthAngularAcceleration();
        Vector3f orient = MathUtil.quaternionToEulerZYX(host.getTruthOrientation());

        float heading = compass.getHeading();
        Vector3f accelSens = accelerometer.getLinearAcceleration();
        Vector3f gyroSens = gyro.getAngularVelocity();
        float rangeSens = rangeBottom.getRange();
        boolean contactSens = contactBottom.isTripped();

        System.out.println("ID: " + host.getModelId() + "  " +
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
                           "active: " + host.isActive());
    }


    @Override
    public void messageReceived(double time, byte[] data, float rxPower) {
    }
}
